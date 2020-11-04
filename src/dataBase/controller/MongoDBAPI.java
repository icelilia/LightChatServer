package dataBase.controller;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.FindIterable;

import java.util.List;

import org.bson.Document;

public class MongoDBAPI {

    /**
     * 获取或新建数据库
     *
     * @param mongoClient  MongoDB连接
     * @param dataBaseName 数据库名
     * @return 数据库对象
     */
    public static MongoDatabase getOrCreateDatabase(MongoClient mongoClient, String dataBaseName) {
        return mongoClient.getDatabase(dataBaseName);
    }

    /**
     * 获取或新建集合
     *
     * @param mongoDatabase  数据库对象
     * @param collectionName 集合名
     * @return 集合对象
     */
    public static MongoCollection<Document> getOrCreateCollection(MongoDatabase mongoDatabase, String collectionName) {
        return mongoDatabase.getCollection(collectionName);
    }

    /**
     * 插入一条数据
     *
     * @param mongoDatabase  数据库对象
     * @param collectionName 集合名
     * @param document       待插入的文档
     */
    public static void insertOneDocument(MongoDatabase mongoDatabase, String collectionName, Document document) {
        MongoCollection<Document> collection = getOrCreateCollection(mongoDatabase, collectionName);
        collection.insertOne(document);
    }

    /**
     * 插入多条数据（暂未使用）
     *
     * @param mongoDatabase  数据库对象
     * @param collectionName 集合名
     * @param documents      待插入的文档List
     */
    public static void insertManyDocument(MongoDatabase mongoDatabase, String collectionName, List<Document> documents) {
        MongoCollection<Document> collection = getOrCreateCollection(mongoDatabase, collectionName);
        collection.insertMany(documents);
    }

    /**
     * 删除一条数据（暂未使用）
     *
     * @param mongoDatabase  数据库对象
     * @param collectionName 集合名
     * @param document       待删除的文档
     */
    public static void deleteOneDocument(MongoDatabase mongoDatabase, String collectionName, Document document) {
        MongoCollection<Document> collection = getOrCreateCollection(mongoDatabase, collectionName);
        collection.deleteOne(document);
    }

    /**
     * 删除多条数据
     *
     * @param mongoDatabase  数据库对象
     * @param collectionName 集合名
     * @param documents      待删除的文档List
     */
    public static void deleteManyDocument(MongoDatabase mongoDatabase, String collectionName, Document documents) {
        MongoCollection<Document> collection = getOrCreateCollection(mongoDatabase, collectionName);
        collection.deleteMany(documents);
    }

    /**
     * 更新一条数据
     *
     * @param mongoDatabase  数据库对象
     * @param collectionName 集合名
     * @param oldDocument    旧文档
     * @param newDocument    新文档
     */
    public static void updateOneDocument(MongoDatabase mongoDatabase, String collectionName, Document oldDocument, Document newDocument) {
        MongoCollection<Document> collection = getOrCreateCollection(mongoDatabase, collectionName);
        collection.replaceOne(oldDocument, newDocument);
    }

    /**
     * 查询一条数据（可能返回null）
     *
     * @param mongoDatabase  数据库对象
     * @param collectionName 集合名
     * @param document       待查找的文档
     * @return 待查找文档的完整信息
     */
    public static Document findOneDocument(MongoDatabase mongoDatabase, String collectionName, Document document) {
        MongoCollection<Document> collection = getOrCreateCollection(mongoDatabase, collectionName);
        FindIterable<Document> documents = collection.find(document);
        return documents.first();
    }

    /**
     * 根据查询条件返回多条匹配结果
     *
     * @param mongoDatabase  数据库对象
     * @param collectionName 集合名
     * @param document       待查找的文档
     * @return 待查找的文档List
     */
    public static FindIterable<Document> findDocument(MongoDatabase mongoDatabase, String collectionName, Document document) {
        MongoCollection<Document> collection = getOrCreateCollection(mongoDatabase, collectionName);
        return collection.find(document);
    }

    /**
     * 获取某个文档的某个字段的最大值
     *
     * @param mongoDatabase  数据库对象
     * @param collectionName 集合名
     * @param fieldName      字段名
     * @return 字段最大值
     */
    public static Document getMaxOfCollection(MongoDatabase mongoDatabase, String collectionName, String fieldName) {
        MongoCollection<Document> collection = getOrCreateCollection(mongoDatabase, collectionName);
        return collection.find().sort(Sorts.orderBy(Sorts.descending(fieldName))).skip(0).limit(1).first();
    }
}
