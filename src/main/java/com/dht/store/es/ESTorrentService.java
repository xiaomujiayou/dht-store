package com.dht.store.es;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import com.dht.store.downloader.bo.Task;
import com.dht.store.entity.File;
import com.dht.store.entity.Magnet;
import com.dht.store.entity.MateData;
import com.dht.store.entity.Torrent;
import com.dht.store.enums.SearchField;
import com.dht.store.exception.ServiceException;
import com.dht.store.utils.BasePage;
import com.dht.store.utils.DLUtil;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class ESTorrentService {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    public Page<Torrent> search(String searchModel, String searchField, String keywords, Pageable pageable) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        if (ObjUtil.isEmpty(keywords)) {
            queryBuilder.withQuery(QueryBuilders.matchAllQuery()).withSorts(SortBuilders.fieldSort("hot").order(SortOrder.DESC));
        } else {
            //高亮设置
            if (StrUtil.isNotBlank(searchField)) {
                queryBuilder.withHighlightBuilder(new HighlightBuilder().preTags("<span style='color:red;'>").postTags("</span>").field(searchField));
            } else
                queryBuilder.withHighlightBuilder(new HighlightBuilder().preTags("<span style='color:red;'>").postTags("</span>").field(SearchField.NAME.getFieldName()).field(SearchField.FILES.getFieldName()));
            if ("term".equals(searchModel)) {      //精确查询
                if (StrUtil.isNotBlank(searchField)) {
                    queryBuilder.withQuery(QueryBuilders.matchQuery(searchField, keywords).operator(Operator.AND).analyzer("ik_max_word"));
                } else {
                    queryBuilder.withQuery(QueryBuilders.boolQuery().should(QueryBuilders.matchQuery(SearchField.NAME.getFieldName(), keywords).operator(Operator.AND).analyzer("ik_max_word")).should(QueryBuilders.matchQuery(SearchField.FILES.getFieldName(), keywords).operator(Operator.AND).analyzer("ik_max_word")));
                }
            } else if ("match".equals(searchModel)) {      //模糊查询
                if (StrUtil.isNotBlank(searchField)) {
                    queryBuilder.withQuery(QueryBuilders.matchQuery(searchField, keywords).operator(Operator.OR).analyzer("ik_max_word"));
                } else {
                    queryBuilder.withQuery(QueryBuilders.boolQuery().should(QueryBuilders.matchQuery(SearchField.NAME.getFieldName(), keywords).operator(Operator.OR).analyzer("ik_max_word")).should(QueryBuilders.matchQuery(SearchField.FILES.getFieldName(), keywords).operator(Operator.OR).analyzer("ik_max_word")));
                }
            } else if ("wildcard".equals(searchModel)) {      //通配符查询
                if (StrUtil.isNotBlank(searchField)) {
                    queryBuilder.withQuery(QueryBuilders.wildcardQuery(searchField, keywords));
                } else {
                    queryBuilder.withQuery(QueryBuilders.boolQuery().should(QueryBuilders.wildcardQuery(SearchField.NAME.getFieldName(), keywords)).should(QueryBuilders.wildcardQuery(SearchField.FILES.getFieldName(), keywords)));
                }
            } else if ("regexp".equals(searchModel)) {      //正则查询
                if (StrUtil.isNotBlank(searchField)) {
                    queryBuilder.withQuery(QueryBuilders.regexpQuery(searchField, keywords));
                } else {
                    queryBuilder.withQuery(QueryBuilders.boolQuery().should(QueryBuilders.regexpQuery(SearchField.NAME.getFieldName(), keywords)).should(QueryBuilders.regexpQuery(SearchField.FILES.getFieldName(), keywords)));
                }
            }
        }
        NativeSearchQuery searchQuery = queryBuilder
                .withSourceFilter(new FetchSourceFilter(null, new String[]{"files"}))
                .withPageable(pageable).build();
        SearchHits<Torrent> searchHits = elasticsearchRestTemplate.search(searchQuery, Torrent.class);
        for (SearchHit<Torrent> searchHit : searchHits.getSearchHits()) {
            if (searchHit.getHighlightFields().containsKey(SearchField.NAME.getFieldName())) {
                searchHit.getContent().setName(searchHit.getHighlightField(SearchField.NAME.getFieldName()).get(0));
            }
            if (searchHit.getHighlightFields().containsKey(SearchField.FILES.getFieldName())) {
                searchHit.getContent().setHighlightFiles(searchHit.getHighlightField(SearchField.FILES.getFieldName()));
            }
        }
        SearchPage<Torrent> searchPage = SearchHitSupport.searchPageFor(searchHits, pageable);
        Page<Torrent> page = (Page<Torrent>) SearchHitSupport.unwrapSearchHits(searchPage);
        if (CollUtil.isNotEmpty(page.getContent())) {
            try {
                List<Task> taskList = DLUtil.getDownloader().getTaskList();
                if (CollUtil.isNotEmpty(taskList)) {
                    for (Torrent torrent : page.getContent()) {
                        a:
                        for (Magnet magnet : torrent.getMagnets()) {
                            for (Task task : taskList) {
                                if (task.getHash().equals(magnet.getHash())) {
                                    torrent.setTask(task);
                                    break a;
                                }
                            }
                        }
                    }
                }
            } catch (ServiceException e) {
                log.warn(e.getMessage());
            }
        }
        return page;
    }

    public Page<File> fileList(String path, Integer pageNum, Integer pageSize, List<MateData.File> files) {
        String[] paths = StrUtil.isBlank(path) ? new String[]{} : path.split("\\|");
        List<File> fileList = getFiles(files, paths);
        int start = PageUtil.getStart(pageNum, pageSize);
        List<File> list = CollUtil.sub(fileList, start, start + pageSize);
        BasePage<File> page = new BasePage<>(list, pageNum, pageSize, fileList.size());
        return page;
    }

    private List<File> getFiles(List<MateData.File> files, String[] paths) {
        if (ObjUtil.isEmpty(files)) return Collections.emptyList();
        Stream<MateData.File> stream = files.stream();
        for (int i = 0; i < paths.length; i++) {
            int index = i;
            stream = stream.filter(o -> {
                if (o.getPath().size() <= index) return false;
                return o.getPath().get(index).equals(paths[index]);
            });
        }
        return stream.map(o -> new File(o, paths)).distinct().sorted(Comparator.comparing(File::isFolder).reversed()).collect(Collectors.toList());
    }
}
