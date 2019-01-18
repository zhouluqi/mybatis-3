/**
 * Copyright 2009-2018 the original author or authors.
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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.ibatis.io.Resources;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class UnpooledDataSource implements DataSource {
    //驱动类加载器
    private ClassLoader driverClassLoader;
    //配置文件驱动属性信息
    private Properties driverProperties;
    //保存已注册的驱动信息映射 类名--对象
    private static Map<String, Driver> registeredDrivers = new ConcurrentHashMap<>();
    //驱动名称（类名）
    private String driver;
    //数据库连接地址
    private String url;
    //数据库用户名
    private String username;
    //数据库密码
    private String password;
    //事物是否自动提交
    private Boolean autoCommit;
    //默认事物级别
    private Integer defaultTransactionIsolationLevel;

    //获取系统当中已存在的驱动信息
    static {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            registeredDrivers.put(driver.getClass().getName(), driver);
        }
    }

    //无参构造方法
    public UnpooledDataSource() {
    }

    //初始化driver、url、username、password
    public UnpooledDataSource(String driver, String url, String username, String password) {
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    //初始化driver、url、username、driverProperties
    public UnpooledDataSource(String driver, String url, Properties driverProperties) {
        this.driver = driver;
        this.url = url;
        this.driverProperties = driverProperties;
    }

    //初始化driverClassLoader、driver、url、username、password
    public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username, String password) {
        this.driverClassLoader = driverClassLoader;
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    //初始化driverClassLoader、driver、url、driverProperties
    public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
        this.driverClassLoader = driverClassLoader;
        this.driver = driver;
        this.url = url;
        this.driverProperties = driverProperties;
    }

    //重写dataSource的getConnection方法返回一个数据库连接
    @Override
    public Connection getConnection() throws SQLException {
        return doGetConnection(username, password);
    }

    //重写dataSource的getConnection方法返回一个数据库连接
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return doGetConnection(username, password);
    }

    //重写CommonDataSource下的方法
    //设置驱动管理的登录超时时间
    @Override
    public void setLoginTimeout(int loginTimeout) {
        DriverManager.setLoginTimeout(loginTimeout);
    }

    //重写CommonDataSource下的方法
    //获取驱动管理的登录超时时间
    @Override
    public int getLoginTimeout() {
        return DriverManager.getLoginTimeout();
    }

    //重写CommonDataSource下的setLogWriter方法
    //设置日志打印输出流
    @Override
    public void setLogWriter(PrintWriter logWriter) {
        DriverManager.setLogWriter(logWriter);
    }

    //重写CommonDataSource下的getLogWriter方法
    //获取日志打印输出流
    @Override
    public PrintWriter getLogWriter() {
        return DriverManager.getLogWriter();
    }

    //获取驱动类加载器
    public ClassLoader getDriverClassLoader() {
        return driverClassLoader;
    }

    //设置驱动类加载器
    public void setDriverClassLoader(ClassLoader driverClassLoader) {
        this.driverClassLoader = driverClassLoader;
    }

    //获取配置文件的驱动信息
    public Properties getDriverProperties() {
        return driverProperties;
    }

    //设置配置文件的驱动信息
    public void setDriverProperties(Properties driverProperties) {
        this.driverProperties = driverProperties;
    }

    //获取驱动
    public String getDriver() {
        return driver;
    }

    //设置驱动
    public synchronized void setDriver(String driver) {
        this.driver = driver;
    }

    //获取路径
    public String getUrl() {
        return url;
    }

    //设置路径
    public void setUrl(String url) {
        this.url = url;
    }

    //获取用户名
    public String getUsername() {
        return username;
    }

    //设置用户名
    public void setUsername(String username) {
        this.username = username;
    }

    //获取密码
    public String getPassword() {
        return password;
    }

    //设置密码
    public void setPassword(String password) {
        this.password = password;
    }

    //是否自动提交
    public Boolean isAutoCommit() {
        return autoCommit;
    }

    //设置是否自动提交
    public void setAutoCommit(Boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    //获取事物隔离级别
    public Integer getDefaultTransactionIsolationLevel() {
        return defaultTransactionIsolationLevel;
    }

    //设置事物隔离级别
    public void setDefaultTransactionIsolationLevel(Integer defaultTransactionIsolationLevel) {
        this.defaultTransactionIsolationLevel = defaultTransactionIsolationLevel;
    }

    //获取连接--设置连接属性阶段，把连接属性driverProperties、user、password放入props中
    private Connection doGetConnection(String username, String password) throws SQLException {
        Properties props = new Properties();
        if (driverProperties != null) {
            props.putAll(driverProperties);
        }
        if (username != null) {
            props.setProperty("user", username);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        //获取连接
        return doGetConnection(props);
    }

    //获取连接--初始化驱动，通过url和properties中的信息。配置连接，返回连接
    private Connection doGetConnection(Properties properties) throws SQLException {
        //初始化驱动
        initializeDriver();
        //获取连接
        Connection connection = DriverManager.getConnection(url, properties);
        //配置连接
        configureConnection(connection);
        //返回连接
        return connection;
    }

    //初始化驱动
    private synchronized void initializeDriver() throws SQLException {
        //注册驱动是否包含该driver,若不存在进行初始化
        if (!registeredDrivers.containsKey(driver)) {
            Class<?> driverType;
            try {
                //获得driver类
                if (driverClassLoader != null) {
                    driverType = Class.forName(driver, true, driverClassLoader);
                } else {
                    driverType = Resources.classForName(driver);
                }
                // DriverManager requires the driver to be loaded via the system ClassLoader.
                // http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
                //实例化驱动
                Driver driverInstance = (Driver) driverType.newInstance();
                //向系统当中注册驱动(代理驱动对象或者驱动接口的一个实现)
                DriverManager.registerDriver(new DriverProxy(driverInstance));
                //向该数据源保存驱动
                registeredDrivers.put(driver, driverInstance);
            } catch (Exception e) {
                throw new SQLException("Error setting driver on UnpooledDataSource. Cause: " + e);
            }
        }
    }

    //配置连接 事物是否自动提交属性和默认事物隔离级别 获取连接就带这个属性(默认的)
    private void configureConnection(Connection conn) throws SQLException {
        if (autoCommit != null && autoCommit != conn.getAutoCommit()) {
            conn.setAutoCommit(autoCommit);
        }
        if (defaultTransactionIsolationLevel != null) {
            conn.setTransactionIsolation(defaultTransactionIsolationLevel);
        }
    }

    //TODO
    //代理驱动
    private static class DriverProxy implements Driver {
        private Driver driver;

        DriverProxy(Driver d) {
            this.driver = d;
        }

        @Override
        public boolean acceptsURL(String u) throws SQLException {
            return this.driver.acceptsURL(u);
        }

        @Override
        public Connection connect(String u, Properties p) throws SQLException {
            return this.driver.connect(u, p);
        }

        @Override
        public int getMajorVersion() {
            return this.driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return this.driver.getMinorVersion();
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
            return this.driver.getPropertyInfo(u, p);
        }

        @Override
        public boolean jdbcCompliant() {
            return this.driver.jdbcCompliant();
        }

        // @Override only valid jdk7+
        public Logger getParentLogger() {
            return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        }
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException(getClass().getName() + " is not a wrapper.");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    // @Override only valid jdk7+
    public Logger getParentLogger() {
        // requires JDK version 1.6
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

}
