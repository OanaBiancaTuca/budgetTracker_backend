package com.example.springapp.chatbot;

import com.example.springapp.chatbot.dto.ChatGPTRequest;
import com.example.springapp.chatbot.dto.ChatGPTResponse;
import com.example.springapp.chatbot.dto.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String OPEN_API_CHAT_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private final Map<String, String> responseCache = new HashMap<>();
    private static final Logger logger = Logger.getLogger(OpenAIService.class.getName());

    public ChatGPTResponse getOpenAIResponse(String prompt) {
        if (responseCache.containsKey(prompt)) {
            String cachedResponse = responseCache.get(prompt);
            ChatGPTResponse response = new ChatGPTResponse();
            response.setChoices(List.of(new ChatGPTResponse.Choice(new Message("assistant", cachedResponse))));
            return response;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        String customizedPrompt = "Ești un expert în economie și investiții. Răspunde la următoarea întrebare ca și cum ai fi un om cunoscător în acest domeniu și răspunde în limba română. " +
                prompt;
        ChatGPTRequest request = new ChatGPTRequest();
        request.setModel("gpt-3.5-turbo");
        request.setMessages(List.of(new Message("user", customizedPrompt)));

        HttpEntity<ChatGPTRequest> entity = new HttpEntity<>(request, headers);
        RestTemplate template = new RestTemplate();

        try {
            ChatGPTResponse response = template.postForObject(OPEN_API_CHAT_ENDPOINT, entity, ChatGPTResponse.class);
            if (response != null) {
                String responseText = response.getChoices().get(0).getMessage().getContent();
                responseCache.put(prompt, responseText);
            }
            return response;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                logger.warning("Rate limit exceeded, retrying after delay...");
                throw e;
            } else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                logger.severe("Forbidden error: " + e.getResponseBodyAsString());
                throw new RuntimeException("Access forbidden. You are not allowed to sample from this model.", e);
            } else if (e.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                logger.severe("Quota exceeded: " + e.getResponseBodyAsString());
                throw new RuntimeException("Quota exceeded. Please check your plan and billing details.", e);
            } else {
                logger.severe("HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
                throw new RuntimeException("HTTP error occurred: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
            }
        } catch (Exception e) {
            logger.severe("Unexpected error: " + e.getMessage());
            throw new RuntimeException("Unexpected error occurred while generating response", e);
        }
    }
}

//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Logger;
//
//@Service
//public class OpenAIService {
//
//    @Value("${openai.api.key}")
//    private String apiKey;
//
//    private static final String OPEN_API_CHAT_ENDPOINT = "https://api.openai.com/v1/chat/completions";
//    private final Map<String, String> responseCache = new HashMap<>();
//    private static final Logger logger = Logger.getLogger(OpenAIService.class.getName());
//
//    private final TransactionRepository transactionRepository;
//
//    public OpenAIService(TransactionRepository transactionRepository) {
//        this.transactionRepository = transactionRepository;
//    }
//
//    public ChatGPTResponse getOpenAIResponse(String prompt, Integer userId) {
//        if (responseCache.containsKey(prompt)) {
//            String cachedResponse = responseCache.get(prompt);
//            ChatGPTResponse response = new ChatGPTResponse();
//            response.setChoices(List.of(new ChatGPTResponse.Choice(new Message("assistant", cachedResponse))));
//            return response;
//        }
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("Authorization", "Bearer " + apiKey);
//
//        // Fetch top transactions from the database
//        List<Transaction> topTransactions = transactionRepository.findTop10ByUser_UserIdOrderByAmountDesc(userId);
//        StringBuilder transactionDetails = new StringBuilder("Cele mai mari tranzacții/cheltuieli sunt:\n");
//        for (Transaction transaction : topTransactions) {
//            transactionDetails.append(String.format("Descriere: %s, Suma: %s, Data: %s\n",
//                    transaction.getDescription(), transaction.getAmount(), transaction.getDateTime()));
//        }
//
//        String customizedPrompt = "Ești un expert în economie și investiții. Răspunde la următoarea întrebare ca și cum ai fi un om cunoscător în acest domeniu și răspunde în limba română. " +
//                prompt + "\n\n" + transactionDetails.toString();
//
//        ChatGPTRequest request = new ChatGPTRequest();
//        request.setModel("gpt-3.5-turbo");
//        request.setMessages(List.of(new Message("user", customizedPrompt)));
//
//        HttpEntity<ChatGPTRequest> entity = new HttpEntity<>(request, headers);
//        RestTemplate template = new RestTemplate();
//
//        try {
//            ChatGPTResponse response = template.postForObject(OPEN_API_CHAT_ENDPOINT, entity, ChatGPTResponse.class);
//            if (response != null) {
//                String responseText = response.getChoices().get(0).getMessage().getContent();
//                responseCache.put(prompt, responseText);
//            }
//            return response;
//        } catch (HttpClientErrorException e) {
//            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
//                logger.warning("Rate limit exceeded, retrying after delay...");
//                throw e;
//            } else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
//                logger.severe("Forbidden error: " + e.getResponseBodyAsString());
//                throw new RuntimeException("Access forbidden. You are not allowed to sample from this model.", e);
//            } else if (e.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
//                logger.severe("Quota exceeded: " + e.getResponseBodyAsString());
//                throw new RuntimeException("Quota exceeded. Please check your plan and billing details.", e);
//            } else {
//                logger.severe("HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
//                throw new RuntimeException("HTTP error occurred: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
//            }
//        } catch (Exception e) {
//            logger.severe("Unexpected error: " + e.getMessage());
//            throw new RuntimeException("Unexpected error occurred while generating response", e);
//        }
//    }
//}
