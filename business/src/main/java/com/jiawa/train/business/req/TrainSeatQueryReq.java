package com.jiawa.train.business.req;

import com.jiawa.train.req.PageReq;

public class TrainSeatQueryReq extends PageReq {
    String trainCode;

    public String getTrainCode() {
        return trainCode;
    }

    public void setTrainCode(String trainCode) {
        this.trainCode = trainCode;
    }

    @Override
    public String toString() {
        return "TrainSeatQueryReq{" +
                "} " + super.toString();
    }
}
