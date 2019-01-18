/**
 * Copyright 2009-2016 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.datasource.jndi;

import java.util.Map.Entry;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.ibatis.datasource.DataSourceException;
import org.apache.ibatis.datasource.DataSourceFactory;

/**
 * @author Clinton Begin
 */
//为了数据源能在容器中使用，可以集中或者容器外部配置数据源，放置一个JNDI上下文引用
    //很少使用此功能
public class JndiDataSourceFactory implements DataSourceFactory {

    //在initialContext中寻找上下文
    public static final String INITIAL_CONTEXT = "initial_context";
    //引用数据源实例位置的上下文的路径，提供了 initial_context 配置时会在其返回的上下文中进行查找，
    // 没有提供时则直接在 InitialContext 中查找。
    public static final String DATA_SOURCE = "data_source";
    //和其他数据源配置类似，可以通过添加前缀“env.”直接把属性传递给初始上下文
    public static final String ENV_PREFIX = "env.";
    //dataSource不在构造方法中创建
    private DataSource dataSource;

    //设置数据源的属性
    @Override
    public void setProperties(Properties properties) {
        try {
            InitialContext initCtx;
            //获取系统的Properties对象
            Properties env = getEnvProperties(properties);
            //创建InitialContext对象(上下文)
            if (env == null) {
                initCtx = new InitialContext();
            } else {
                initCtx = new InitialContext(env);
            }

            // 从 InitialContext 上下文中，获取 DataSource 对象
            if (properties.containsKey(INITIAL_CONTEXT)
                    && properties.containsKey(DATA_SOURCE)) {
                Context ctx = (Context) initCtx.lookup(properties.getProperty(INITIAL_CONTEXT));
                dataSource = (DataSource) ctx.lookup(properties.getProperty(DATA_SOURCE));
            } else if (properties.containsKey(DATA_SOURCE)) {
                dataSource = (DataSource) initCtx.lookup(properties.getProperty(DATA_SOURCE));
            }
        } catch (NamingException e) {
            throw new DataSourceException("There was an error configuring JndiDataSourceTransactionPool. Cause: " + e, e);
        }
    }

    //返回数据源对象
    @Override
    public DataSource getDataSource() {
        return dataSource;
    }
    //获取系统的Properties对象
    private static Properties getEnvProperties(Properties allProps) {
        final String PREFIX = ENV_PREFIX;
        Properties contextProperties = null;
        for (Entry<Object, Object> entry : allProps.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (key.startsWith(PREFIX)) {
                if (contextProperties == null) {
                    contextProperties = new Properties();
                }
                contextProperties.put(key.substring(PREFIX.length()), value);
            }
        }
        return contextProperties;
    }

}
