package com.fubt.entity;

import java.util.Date;

/**
 * <p>@Description: </p>
 *
 * @date 2019/5/1410:20 PM
 */
public class RobotConfig {

    private Integer id ;
    private Integer status ;
    private Integer userId ;
    private Long frequence ;
    private Double maxNum ;
    private Double maxPrice ;
    private Double minNum ;
    private Double minPrice ;
    private double range;
    private int cycle;
    private double targetPrice;
    private Integer maxBuyOrders;
    private Integer maxSellOrders;
    private String name ;
    private String symbol ;
    private Date createTime ;

    public RobotConfig() {
    }

    public Integer getMaxBuyOrders() {
        return maxBuyOrders;
    }

    public void setMaxBuyOrders(Integer maxBuyOrders) {
        this.maxBuyOrders = maxBuyOrders;
    }

    public Integer getMaxSellOrders() {
        return maxSellOrders;
    }

    public void setMaxSellOrders(Integer maxSellOrders) {
        this.maxSellOrders = maxSellOrders;
    }

    public Integer getId(){
        return  id;
    }
    public void setId(Integer id ){
        this.id = id;
    }

    public Integer getStatus(){
        return  status;
    }
    public void setStatus(Integer status ){
        this.status = status;
    }

    public Integer getUserId(){
        return  userId;
    }
    public void setUserId(Integer userId ){
        this.userId = userId;
    }

    public Long getFrequence(){
        return  frequence;
    }
    public void setFrequence(Long frequence ){
        this.frequence = frequence;
    }

    public Double getMaxNum() {
        return maxNum;
    }

    public void setMaxNum(Double maxNum) {
        this.maxNum = maxNum;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Double getMinNum() {
        return minNum;
    }

    public void setMinNum(Double minNum) {
        this.minNum = minNum;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }

    public String getName(){
        return  name;
    }
    public void setName(String name ){
        this.name = name;
    }

    public String getSymbol(){
        return  symbol;
    }
    public void setSymbol(String symbol ){
        this.symbol = symbol;
    }

    public Date getCreateTime(){
        return  createTime;
    }
    public void setCreateTime(Date createTime ){
        this.createTime = createTime;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public int getCycle() {
        return cycle;
    }

    public void setCycle(int cycle) {
        this.cycle = cycle;
    }

    public double getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(double targetPrice) {
        this.targetPrice = targetPrice;
    }
}
