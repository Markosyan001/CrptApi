package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final Semaphore semaphore;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final long timePeriodMillis;
    private long nextRequestTime;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.timePeriodMillis = timeUnit.toMillis(1);
        this.nextRequestTime = System.currentTimeMillis();
    }

    public synchronized void createDocument(Document document, String signature) throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        if (currentTime < nextRequestTime) {
            Thread.sleep(nextRequestTime - currentTime);
        }

        semaphore.acquire();
        try {
            sendRequest(document, signature);
        } finally {
            semaphore.release();
            if (semaphore.availablePermits() == 0) {
                nextRequestTime = System.currentTimeMillis() + timePeriodMillis;
            }
        }
    }

    private void sendRequest(Document document, String signature) {
        try {
            String json = objectMapper.writeValueAsString(document);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;
    }

    public static class Description {
        public String participantInn;
    }

    public static class Product {
        public String certificate_document;
        public String certificate_document_date;
        public String certificate_document_number;
        public String owner_inn;
        public String producer_inn;
        public String production_date;
        public String tnved_code;
        public String uit_code;
        public String uitu_code;
    }

    public static void main(String[] args) {
        TimeUnit timeUnit = TimeUnit.MINUTES;
        int requestLimit = 5;

        CrptApi crptApi = new CrptApi(timeUnit, requestLimit);

        CrptApi.Document document = new CrptApi.Document();
        document.doc_id = "12345";
        document.doc_status = "NEW";
        document.doc_type = "LP_INTRODUCE_GOODS";
        document.importRequest = true;
        document.owner_inn = "1234567890";
        document.participant_inn = "1234567890";
        document.producer_inn = "1234567890";
        document.production_date = "2020-01-23";
        document.production_type = "TYPE_A";
        document.reg_date = "2020-01-23";
        document.reg_number = "REG12345";

        CrptApi.Description description = new CrptApi.Description();
        description.participantInn = "1234567890";
        document.description = description;

        CrptApi.Product product = new CrptApi.Product();
        product.certificate_document = "CERT12345";
        product.certificate_document_date = "2020-01-23";
        product.certificate_document_number = "CERT_NUM";
        product.owner_inn = "1234567890";
        product.producer_inn = "1234567890";
        product.production_date = "2020-01-23";
        product.tnved_code = "TNVED1234";
        product.uit_code = "UITCODE1234";
        product.uitu_code = "UITUCODE1234";

        document.products = new CrptApi.Product[]{ product };

        String signature = "exampleSignature";

        try {
            crptApi.createDocument(document, signature);
            System.out.println("Документ успешно отправлен.");
        } catch (InterruptedException e) {
            System.err.println("Ошибка при отправке документа: " + e.getMessage());
        }
    }
}
