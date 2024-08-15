package com.dht.store.message;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.dht.store.entity.Magnet;
import com.dht.store.entity.MateData;
import com.dht.store.entity.Torrent;
import com.dht.store.es.ESTorrentRepo;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class DhtMessageReceiver {

    //新增数量
    public static int COUNT = 0;
    //违规数量
    public static int ILLEGAL_COUNT = 0;

    @Autowired
    private ESTorrentRepo esTorrentRepo;

    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange("dht"),
            key = "torrent",
            value = @Queue("torrent.queue")
    ))
    public void onMessageIpv4(Channel channel, Message message) throws IOException {
        onMessage(channel, message);
    }

    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange("dht"),
            key = "torrent_ipv6",
            value = @Queue("torrent.ipv6.queue")
    ))
    public void onMessageIpv6(Channel channel, Message message) throws IOException {
        onMessage(channel, message);
    }

    public void onMessage(Channel channel, Message message) throws IOException {
        long msgId = message.getMessageProperties().getDeliveryTag();
        try {
            MateData mateData = JSON.parseObject(message.getBody(),MateData.class);
            if (mateData.isIllegal()) {
                ILLEGAL_COUNT++;
                log.warn("违规资源过滤：{}", mateData.getName());
                return;
            }
            long length = mateData.getLength() != null ? mateData.getLength() : mateData.getFiles().stream().mapToLong(MateData.File::getLength).sum();
            mateData.setLength(length);
            String hash = HexUtil.encodeHexStr(DigestUtil.sha1(mateData.getName() + length));
            Optional<Torrent> byId = esTorrentRepo.findById(hash);
            if (byId.isPresent()) {
                Torrent old = byId.get();
                updateMagnet(old, mateData.getInfoHash());
                esTorrentRepo.save(old);
            } else {
                Torrent torrent = new Torrent();
                torrent.setHash(hash);
                torrent.setName(mateData.getName());
                torrent.setFiles(mateData.getFiles());
                torrent.setLength(mateData.getLength());
                torrent.setCreateTime(new Date());
                updateMagnet(torrent, mateData.getInfoHash());
                esTorrentRepo.save(torrent);
                COUNT++;
            }
        } catch (Exception e) {
            channel.basicReject(msgId, false);
            e.printStackTrace();
        } finally {
            channel.basicAck(msgId, false);
        }
    }

    private void updateMagnet(Torrent torrent, String infoHash) {
        List<Magnet> magnets = torrent.getMagnets() == null ? new ArrayList<>() : torrent.getMagnets();
        torrent.setMagnets(magnets);
        Magnet oldMagnet = magnets.stream().filter(magnet -> magnet.getHash().equals(infoHash)).findFirst().orElse(null);
        if (oldMagnet == null) {
            Magnet magnet = new Magnet();
            magnet.setHash(infoHash);
            magnet.setHot(0);
            magnet.setCreateTime(new Date());
            magnets.add(magnet);
        } else {
            oldMagnet.setHot(oldMagnet.getHot() + 1);
            oldMagnet.setUpdateTime(new Date());
        }
        torrent.setHot(magnets.stream().mapToLong(Magnet::getHot).sum());
        torrent.setUpdateTime(new Date());
    }
}
