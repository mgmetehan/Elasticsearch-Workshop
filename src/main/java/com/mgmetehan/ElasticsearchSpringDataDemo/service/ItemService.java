package com.mgmetehan.ElasticsearchSpringDataDemo.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.mgmetehan.ElasticsearchSpringDataDemo.model.Item;
import com.mgmetehan.ElasticsearchSpringDataDemo.repository.ItemRepository;
import com.mgmetehan.ElasticsearchSpringDataDemo.util.ESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final JsonDataService jsonDataService;
    private final ElasticsearchClient elasticsearchClient;

    public Item createIndex(Item item) {
        return itemRepository.save(item);
    }

    public void addItemsFromJson() {
        log.info("Adding Items from Json");
        List<Item> Items = jsonDataService.readItemsFromJson();
        for (Item Item : Items) {
            itemRepository.save(Item);
        }
    }

    public Iterable<Item> getItems() {
        log.info("Getting Items");
        return itemRepository.findAll();
    }

    public String matchAllServices() throws IOException {
        Supplier<Query> supplier = ESUtil.supplier();
        SearchResponse<Map> searchResponse = elasticsearchClient.search(s -> s.query(supplier.get()), Map.class);
        log.info("elasticsearch query is " + supplier.get().toString());
        log.info("elasticsearch response is " + searchResponse.toString());
        return searchResponse.hits().hits().toString();
    }

    public List<Item> matchAllItemsServices() {
        try {
            Supplier<Query> supplier = ESUtil.supplier();
            SearchResponse<Item> searchResponse = elasticsearchClient.search(s -> s.index("items_index").query(supplier.get()), Item.class);
            log.info("elasticsearch query is " + supplier.get().toString());

            log.info("elasticsearch response is " + searchResponse.toString());
            List<Hit<Item>> listOfHits = searchResponse.hits().hits();
            List<Item> listOfItems = new ArrayList<>();
            for (Hit<Item> hit : listOfHits) {
                listOfItems.add(hit.source());
            }
            return listOfItems;
        } catch (IOException e) {
            log.error("Error while getting all items", e);
            throw new RuntimeException(e);
        }
    }

    public SearchResponse<Item> searchName(String fieldValue) {
        try {
            Supplier<Query> supplier = ESUtil.supplierWithNameField(fieldValue);
            SearchResponse<Item> searchResponse = elasticsearchClient.search(s -> s.index("items_index").query(supplier.get()), Item.class);
            log.info("elasticsearch query is " + supplier.get().toString());
            return searchResponse;
        } catch (IOException e) {
            log.error("Error while getting all items", e);
            throw new RuntimeException(e);
        }
    }

    public List<Item> searchItemsWithQuery(String name, String brand) {
        try {
            return itemRepository.searchNameAndBrand(name, brand);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public SearchResponse<Item> boolQuery(String name, String brand) throws IOException {
        Supplier<Query> supplier = ESUtil.supplierQueryForBoolQuery(name, brand);
        SearchResponse<Item> searchResponse = elasticsearchClient.search(s -> s.index("items_index").query(supplier.get()), Item.class);
        log.info("elasticsearch query is " + supplier.get().toString());
        return searchResponse;
    }

    public SearchResponse<Item> autoSuggestItem(String name) throws IOException {
        Supplier<Query> supplier = ESUtil.createSupplierAutoSuggest(name);
        SearchResponse<Item> searchResponse = elasticsearchClient
                .search(s -> s.index("items_index").query(supplier.get()), Item.class);
        log.info(" elasticsearch auto suggestion query" + supplier.get().toString());
        return searchResponse;
    }
}
