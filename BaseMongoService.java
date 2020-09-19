package com.tongxiang.backend.service;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serializable;
import java.util.List;

/**
 * @author XuanWem Chen
 * @email sproutgod667@gmail.com
 * @date 2020/7/11 23:44
 */

public interface BaseMongoService<T, ID extends Serializable> {

    /**
     * 保存对象
     *
     * @param t
     * @return
     */
    T save(T t);

    /**
     * 保存 List<对象>
     *
     * @param var1
     * @return
     */
    List<T> saveAll(Iterable<T> var1);

    /**
     * 通过id查找到唯一的对象
     *
     * @param id
     * @return
     */
    T findById(ID id);

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    void deleteById(ID id);

    /**
     * 通过id删除
     *
     * @param list
     * @return
     */
    void deleteAll(List<T> list);

    /**
     * 通过对象删
     *
     * @param t
     * @return
     */
    void delete(T t);

    /**
     * 通过id查找是否存在该记录
     *
     * @param id
     * @return
     */
    boolean existsById(ID id);

    /**
     * 表中的记录数
     *
     * @return
     */
    long count();

    /**
     * 表中的所有记录
     *
     * @return
     */
    List<T> findAll();

    /**
     * 通过对象查询表中的所有记录
     *
     * @return
     */
    List<T> findAll(T t);

    /**
     * 查找表中所有数据且排序
     *
     * @param var1
     * @return
     */
    List<T> findAll(Sort var1);

    /**
     * 通过list<id> 查找对象集合
     *
     * @param var1
     * @return
     */
    Iterable<T> findAllById(Iterable<ID> var1);

    /**
     * 构造对象条件查找
     *
     * @param var1
     * @return
     */
    List<T> findAll(Example<T> var1);

    /**
     * 构造对象条件查找 且排序
     *
     * @param var1
     * @param var2
     * @return
     */
    List<T> findAll(Example<T> var1, Sort var2);

    /**
     * 分页查找
     *
     * @param var1
     * @return
     */
    Page<T> findAll(Pageable var1);

    /**
     * 分页查找 且带上条件
     *
     * @param var1
     * @param var2
     * @return
     */
    Page<T> findAll(Example<T> var1, Pageable var2);

    /**
     * 分页查找 且带上条件
     *
     * @param var1
     * @param var2
     * @return
     */
    Page<T> findAll(T var1, Pageable var2);

}