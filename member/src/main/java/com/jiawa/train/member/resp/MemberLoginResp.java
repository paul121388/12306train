package com.jiawa.train.member.resp;

public class MemberLoginResp {
    private Long id;
    private String mobile;




    @Override
    public String toString() {
        return "MemberLoginResp{" +
                "mobile='" + mobile + '\'' +
                ", id=" + id +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

}