package com.example.model;
/*
这是一个日程类
*/



public class Schedule {
    // 唯一id
    private int id;
    //日程名称
    private String name;
    //日程描述
    private String description;
    //截止日期
    private String dueDate;
    //是否完成
    private boolean completed;
    //创建时间
    private String createdAt;
    //更新时间
    private String updatedAt;

    //空参构造函数
    public Schedule() {

    }
    // 构造函数
    public Schedule(int id, String name, String description, String dueDate, boolean completed, String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.dueDate = dueDate;
        this.completed = completed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt; 
    }
    //缺少id、createdAt和updatedAt的构造函数
    public Schedule(String name, String description, String dueDate, boolean completed) {
        this.name = name;
        this.description = description;
        this.dueDate = dueDate;
        this.completed = completed;
    }
    public int getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public String getDescription(){
        return description;
    }
    public String getDueDate(){
        return dueDate;
    }
    public boolean isCompleted(){
        return completed;
    }
    public String getCreatedAt(){
        return createdAt;
    }
    public String getUpdatedAt(){
        return updatedAt;
    }
    public void setId(int id){
        this.id = id;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setDescription(String description){
        this.description = description;
    }

    public void setDueDate(String dueDate){
        this.dueDate = dueDate;
    }
    public void setCompleted(boolean completed){
        this.completed = completed;
    }
    public void setCreatedAt(String createdAt){
        this.createdAt = createdAt;
    }
    public void setUpdatedAt(String updatedAt){
        this.updatedAt = updatedAt; 
    }
    
   }   


