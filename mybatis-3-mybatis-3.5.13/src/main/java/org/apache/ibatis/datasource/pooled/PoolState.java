/*
 *    Copyright 2009-2023 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.datasource.pooled;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Clinton Begin
 */
public class PoolState {

//    真正要管理的数据源对象
    protected PooledDataSource dataSource;
//    记录池化的连接
    protected final List<PooledConnection> idleConnections = new ArrayList<>();
//    记录活跃的连接
    protected final List<PooledConnection> activeConnections = new ArrayList<>();
//    统计请求次数
    protected long requestCount;
//    统计获取连接的累计时间
    protected long accumulatedRequestTime;
//    统计从拿走到归还累计时间
    protected long accumulatedCheckoutTime;
//    统计超时归还的个数
    protected long claimedOverdueConnectionCount;
//    统计累计超时的时间
    protected long accumulatedCheckoutTimeOfOverdueConnections;
//    统计类型等待时间
    protected long accumulatedWaitTime;
//    统计等待次数
    protected long hadToWaitCount;
//    统计无效连接个数
    protected long badConnectionCount;

    public PoolState(PooledDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public synchronized long getRequestCount() {
        return requestCount;
    }

    public synchronized long getAverageRequestTime() {
        return requestCount == 0 ? 0 : accumulatedRequestTime / requestCount;
    }

    public synchronized long getAverageWaitTime() {
        return hadToWaitCount == 0 ? 0 : accumulatedWaitTime / hadToWaitCount;

    }

    public synchronized long getHadToWaitCount() {
        return hadToWaitCount;
    }

    public synchronized long getBadConnectionCount() {
        return badConnectionCount;
    }

    public synchronized long getClaimedOverdueConnectionCount() {
        return claimedOverdueConnectionCount;
    }

    public synchronized long getAverageOverdueCheckoutTime() {
        return claimedOverdueConnectionCount == 0 ? 0
            : accumulatedCheckoutTimeOfOverdueConnections / claimedOverdueConnectionCount;
    }

    public synchronized long getAverageCheckoutTime() {
        return requestCount == 0 ? 0 : accumulatedCheckoutTime / requestCount;
    }

    public synchronized int getIdleConnectionCount() {
        return idleConnections.size();
    }

    public synchronized int getActiveConnectionCount() {
        return activeConnections.size();
    }

    @Override
    public synchronized String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n===CONFIGURATION==============================================");
        builder.append("\n jdbcDriver                     ").append(dataSource.getDriver());
        builder.append("\n jdbcUrl                        ").append(dataSource.getUrl());
        builder.append("\n jdbcUsername                   ").append(dataSource.getUsername());
        builder.append("\n jdbcPassword                   ")
            .append(dataSource.getPassword() == null ? "NULL" : "************");
        builder.append("\n poolMaxActiveConnections       ").append(dataSource.poolMaximumActiveConnections);
        builder.append("\n poolMaxIdleConnections         ").append(dataSource.poolMaximumIdleConnections);
        builder.append("\n poolMaxCheckoutTime            ").append(dataSource.poolMaximumCheckoutTime);
        builder.append("\n poolTimeToWait                 ").append(dataSource.poolTimeToWait);
        builder.append("\n poolPingEnabled                ").append(dataSource.poolPingEnabled);
        builder.append("\n poolPingQuery                  ").append(dataSource.poolPingQuery);
        builder.append("\n poolPingConnectionsNotUsedFor  ").append(dataSource.poolPingConnectionsNotUsedFor);
        builder.append("\n ---STATUS-----------------------------------------------------");
        builder.append("\n activeConnections              ").append(getActiveConnectionCount());
        builder.append("\n idleConnections                ").append(getIdleConnectionCount());
        builder.append("\n requestCount                   ").append(getRequestCount());
        builder.append("\n averageRequestTime             ").append(getAverageRequestTime());
        builder.append("\n averageCheckoutTime            ").append(getAverageCheckoutTime());
        builder.append("\n claimedOverdue                 ").append(getClaimedOverdueConnectionCount());
        builder.append("\n averageOverdueCheckoutTime     ").append(getAverageOverdueCheckoutTime());
        builder.append("\n hadToWait                      ").append(getHadToWaitCount());
        builder.append("\n averageWaitTime                ").append(getAverageWaitTime());
        builder.append("\n badConnectionCount             ").append(getBadConnectionCount());
        builder.append("\n===============================================================");
        return builder.toString();
    }

}
