/**
 * Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.datasource.unpooled;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.DataSourceException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * @author Clinton Begin
 */
//非池化的DataSourceFactory实现类---非池化为没有实现数据连接池
public class UnpooledDataSourceFactory implements DataSourceFactory {
    //以driver.开头的字符串
    private static final String DRIVER_PROPERTY_PREFIX = "driver.";
    //上面字符串长度
    private static final int DRIVER_PROPERTY_PREFIX_LENGTH = DRIVER_PROPERTY_PREFIX.length();
    //数据源引用
    protected DataSource dataSource;

    //构造方法实例化一个非池化的数据源赋值给数据源引用
    public UnpooledDataSourceFactory() {
        this.dataSource = new UnpooledDataSource();
    }

    //重写setProperties方法
    @Override
    public void setProperties(Properties properties) {
        //存放数据库驱动信息 driver
        Properties driverProperties = new Properties();
        //MetaObject对象包装器 https://blog.csdn.net/mz4138/article/details/81671319
        //简介：MetaObject是Mybatis提供的一个用于方便、优雅访问对象属性的对象，通过它可以简化代码、
        // 不需要try/catch各种reflect异常,同时它支持对JavaBean、Collection、Map三种类型对象的操作。
        MetaObject metaDataSource = SystemMetaObject.forObject(dataSource);
        for (Object key : properties.keySet()) {
            String propertyName = (String) key;
            //把驱动信息放入driverProperties
            if (propertyName.startsWith(DRIVER_PROPERTY_PREFIX)) {
                String value = properties.getProperty(propertyName);
                driverProperties.setProperty(propertyName.substring(DRIVER_PROPERTY_PREFIX_LENGTH), value);
            } else if (metaDataSource.hasSetter(propertyName)) {
                String value = (String) properties.get(propertyName);
                //通过对dataSource属性值类型转换Properties配置属性值
                Object convertedValue = convertValue(metaDataSource, propertyName, value);
                //将转换后的值赋值给dataSource属性
                metaDataSource.setValue(propertyName, convertedValue);
            } else {
                throw new DataSourceException("Unknown DataSource property: " + propertyName);
            }
        }
        //将驱动信息设置给dataSource
        if (driverProperties.size() > 0) {
            metaDataSource.setValue("driverProperties", driverProperties);
        }
    }

    //重写返回getDataSource方法返回dataSource
    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    //对配置文件信息根据dataSource属性值类型转换以便赋值
    private Object convertValue(MetaObject metaDataSource, String propertyName, String value) {
        Object convertedValue = value;
        // 获得该属性的 setting 方法的参数类型
        Class<?> targetType = metaDataSource.getSetterType(propertyName);
        if (targetType == Integer.class || targetType == int.class) {
            convertedValue = Integer.valueOf(value);
        } else if (targetType == Long.class || targetType == long.class) {
            convertedValue = Long.valueOf(value);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            convertedValue = Boolean.valueOf(value);
        }
        return convertedValue;
    }

}
