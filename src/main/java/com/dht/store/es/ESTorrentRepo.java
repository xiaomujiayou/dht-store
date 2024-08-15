package com.dht.store.es;

import com.dht.store.entity.Torrent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ESTorrentRepo extends ElasticsearchRepository<Torrent, String> {

    //    @Highlight(fields = {@HighlightField(name = "name"),@HighlightField(name = "files")})
    Page<Torrent> findByName(Pageable pageable, String name);
    Page<Torrent> findByFiles(Pageable pageable,String files);

}
