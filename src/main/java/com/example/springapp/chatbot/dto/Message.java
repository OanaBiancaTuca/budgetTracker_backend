package com.example.springapp.chatbot.dto;

public class Message {
    private String role;
    private String content;

    public Message() {
        // Constructor implicit necesar pentru deserializare
    }

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
