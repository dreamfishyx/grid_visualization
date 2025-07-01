package com.dreamfish.backend.dao;

import com.dreamfish.backend.entity.Worker;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2025/4/11 15:38
 */
@Mapper
public interface WorkerMapper {
    /**
     * 获取维修人员列表
     *
     * @param worker 查询条件信息
     * @return 维修人员列表
     */
    List<Worker> getWorkers(Worker worker);

    /**
     * 根据维修人员id查询维修人员
     *
     * @param workerId 维修人员id
     * @return 维修人员信息
     */
    Worker getWorker(@Param("workerId") Integer workerId);

    /**
     * 新增维修人员
     *
     * @param worker 维修人员信息
     * @return 自增主键
     */
    int insertWorker(Worker worker);

    /**
     * 更新维修人员信息
     *
     * @param worker 维修人员信息
     * @return 影响行数
     */
    int updateWorkerInfo(Worker worker);

    /**
     * 删除维修人员
     *
     * @param worker 围嘴人员信息
     * @return 影响行数
     */
    int deleteWorker(Worker worker);

    /**
     * 更新维修人员状态
     *
     * @param worker 维修人员信息
     * @return 影响行数
     */
    int updateWorkerStatus(Worker worker);

    /**
     * 获取一个空闲的维修人员
     *
     * @return 维修人员
     */
    Worker getFirstFreeWorker();

}
