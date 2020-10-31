package dataBase.controller;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.FindIterable;

import java.util.List;

import org.bson.Document;

public class MongoDBAPI {

    // 获取或新建数据库
    public static MongoDatabase getOrCreateDatabase(MongoClient mongoClient, String dataBaseName) {
        return mongoClient.getDatabase(dataBaseName);
    }

    // 获取或新建集合
    public static MongoCollection<Document> getOrCreateCollection(MongoDatabase mongoDatabase, String collectionName) {
        return mongoDatabase.getCollection(collectionName);
    }

    // 插入一条数据
    public static void insertOneDocument(MongoDatabase mongoDatabase, String collectionName, Document document) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        collection.insertOne(document);
    }

    // 插入多条数据
    public static void insertManyDocument(MongoDatabase mongoDatabase, String collectionName, List<Document> documents) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        collection.insertMany(documents);
    }

    // 删除一条数据
    public static void deleteOneDocument(MongoDatabase mongoDatabase, String collectionName, Document document) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        collection.deleteOne(document);
    }

    // 删除多条数据
    public static void deleteManyDocument(MongoDatabase mongoDatabase, String collectionName, Document document) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        collection.deleteMany(document);
    }

    // 更新一条数据
    public static void updateOneDocument(MongoDatabase mongoDatabase, String collectionName, Document oldDocument,
                                         Document newDocument) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        collection.replaceOne(oldDocument, newDocument);
    }

    // 查询一条数据（这个API我觉得8行，只能用于只有一个或零个结果的查找）
    public static Document findOneDocument(MongoDatabase mongoDatabase, String collectionName, Document document) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        FindIterable<Document> documents = collection.find(document);
        return documents.first();
    }

    // 根据查询条件返回多条匹配结果
    public static FindIterable<Document> findDocument(MongoDatabase mongoDatabase, String collectionName, Document document) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        return collection.find(document);
    }

    // 获取某个文档的某个字段的最大值
    public static Document getMaxOfCollection(MongoDatabase mongoDatabase, String collectionName, String fieldName) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        return collection.find().sort(Sorts.orderBy(Sorts.descending(fieldName))).skip(0).limit(1).first();
    }
}
