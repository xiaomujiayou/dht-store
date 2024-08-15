package com.dht.store.entity;

import com.dht.store.downloader.bo.Task;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class TranTask extends Task {
    private String id;
    private String name;
    private String hashString;
    private String totalSize;
    private String percentDone;
    private String addedDate;
    private String trackerStats;
    private String leftUntilDone;
    private String rateDownload;
    private String rateUpload;
    private String recheckProgress;
    private String peersGettingFromUs;
    private String peersSendingToUs;
    private String uploadRatio;
    private String uploadedEver;
    private String downloadedEver;
    private String downloadDir;
    private String error;
    private String errorString;
    private String doneDate;
    private String queuePosition;
    private String activityDateprivate;
}
