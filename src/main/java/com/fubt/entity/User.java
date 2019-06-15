package com.fubt.entity;

import org.beetl.sql.core.annotatoin.Table;

import java.util.Date;

/**
 * <p>@Description: </p>
 *
 * @date 2019/5/1410:19 PM
 */
@Table(name="user")
public class User {

    private Integer id ;
    private String accessKey ;
    private String account ;
    private String pwd;
    private String transPwd;
    private String secretKey ;
    private Date createTime ;

    public User() {
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getTransPwd() {
        return transPwd;
    }

    public void setTransPwd(String transPwd) {
        this.transPwd = transPwd;
    }

    public Integer getId(){
        return  id;
    }
    public void setId(Integer id ){
        this.id = id;
    }

    public String getAccessKey(){
        return  accessKey;
    }
    public void setAccessKey(String accessKey ){
        this.accessKey = accessKey;
    }

    public String getAccount(){
        return  account;
    }
    public void setAccount(String account ){
        this.account = account;
    }

    public String getSecretKey(){
        return  secretKey;
    }
    public void setSecretKey(String secretKey ){
        this.secretKey = secretKey;
    }

    public Date getCreateTime(){
        return  createTime;
    }
    public void setCreateTime(Date createTime ){
        this.createTime = createTime;
    }
}
