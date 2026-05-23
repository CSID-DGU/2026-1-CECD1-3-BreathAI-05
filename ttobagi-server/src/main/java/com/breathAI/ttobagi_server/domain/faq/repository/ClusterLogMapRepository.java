package com.breathAI.ttobagi_server.domain.faq.repository;

import com.breathAI.ttobagi_server.domain.faq.entity.Cluster;
import com.breathAI.ttobagi_server.domain.faq.entity.ClusterLogMap;
import com.breathAI.ttobagi_server.domain.dashboard.entity.AnalysisJob;
import com.breathAI.ttobagi_server.domain.faq.entity.ClusterLogMap.LogType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ClusterLogMapRepository extends JpaRepository<ClusterLogMap, Long> {

    // 기본 조회
    List<ClusterLogMap> findByCluster(Cluster cluster);

    // ID 기반 조회
    List<ClusterLogMap> findByClusterClusterId(Long clusterId);

    // 타입별 필터링
    List<ClusterLogMap> findByLogType(LogType logType);

    // 복합 조건 조회
    List<ClusterLogMap> findByClusterAndLogType(Cluster cluster, LogType logType);

    // 특정 군집에 속한 모든 매핑 삭제
    @Modifying
    @Transactional
    void deleteByClusterIn(List<Cluster> clusters);

    List<ClusterLogMap> findAllByCluster_AnalysisJob(AnalysisJob analysisJob);

    long countByCluster_AnalysisJobAndLogType(AnalysisJob analysisJob, LogType logType);
}