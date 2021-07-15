package utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class SpineApi {

    public static String url = "http://10.2.174.241:10110";

    public static String host = "10.2.174.241";

    public static String loginURI = "/users/login";

    public static String worksheetURI = "/auth/ws/worksheets";

    public static String executeURI = "/auth/ws/execute";

    public static String resultURI = "/auth/ws/history/preview";

    public static String uploadURI = "/auth/ds/put";

    public static String token;

    public static String companyId;

    public static String roleId;

    public static String worksheet;

    public static String database = "";

    public static String schema = "";

    /**
     * 用户登录，保存token\companyId\roleId为全局静态变量
     * @param email
     * @param password
     * @throws Exception
     */
    public static void login(String email, String password) throws Exception {

        JSONObject request = new JSONObject();
        request.put("email", email);
        request.put("password", password);

        // 创建Httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            // 创建Http Post请求
            HttpPost httpPost = new HttpPost(url+loginURI);
            // 创建请求内容
            StringEntity entity = new StringEntity(request.toString(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            // 执行http请求
            response = httpClient.execute(httpPost);
            String resultString = EntityUtils.toString(response.getEntity(), "utf-8");
            JSONObject resultJson = JSONObject.parseObject(new String(resultString));
            token = resultJson.getJSONObject("data").getString("token");
            companyId = resultJson.getJSONObject("data").getString("companyId");
            roleId = resultJson.getJSONObject("data").getString("roleId");
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Login fail!");
        } finally {
            response.close();
        }
    }

    /**
     * 创建worksheet并保存为全局静态变量
     * @throws Exception
     */
    public static void getWorksheet() throws Exception {
        // 创建Httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        JSONObject request = new JSONObject();
        CloseableHttpResponse response = null;
        try {
            // 创建Http Post请求
            HttpPost httpPost = new HttpPost(url+worksheetURI);
            // 创建请求内容
            StringEntity entity = new StringEntity(request.toString(), ContentType.APPLICATION_JSON);
            httpPost.addHeader("token", token);
            httpPost.addHeader("company", companyId);
            httpPost.addHeader("role", roleId);
            httpPost.setEntity(entity);
            // 执行http请求
            response = httpClient.execute(httpPost);
            String resultString = EntityUtils.toString(response.getEntity(), "utf-8");
            JSONObject resultJson = JSONObject.parseObject(new String(resultString));
            worksheet = resultJson.getString("data");
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Apply worksheet fail!");
        } finally {
            response.close();
        }
    }

    /**
     * 异步执行Sql语句，返回runnerId
     * @param sql
     * @return runnerId
     * @throws Exception
     */
    public static String execute(String sql) throws Exception {
        // 创建Httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        JSONObject request = new JSONObject();
        request.put("catalog", database);
        request.put("schema", schema);
        request.put("worksheet", worksheet);
        request.put("sql", sql);

        String runner = null;
        CloseableHttpResponse response = null;
        try {
            // 创建Http Post请求
            HttpPost httpPost = new HttpPost(url+executeURI);
            httpPost.addHeader("token", token);
            httpPost.addHeader("company", companyId);
            httpPost.addHeader("role", roleId);
            // 创建请求内容
            StringEntity entity = new StringEntity(request.toString(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            // 执行http请求
            response = httpClient.execute(httpPost);
            String resultString = EntityUtils.toString(response.getEntity(), "utf-8");
            JSONObject resultJson = JSONObject.parseObject(new String(resultString));
            runner = resultJson.getJSONObject("data").getString("runner");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            response.close();
        }
        return runner;
    }

    /**
     * 异步执行sql语句，若有结果则输出结果，有异常则输出异常
     * @param sql
     * @return
     * @throws Exception
     */
    public static String executeAndGetResult(String sql) throws Exception {
        // 创建Httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        JSONObject request = new JSONObject();
        request.put("catalog", database);
        request.put("schema", schema);
        request.put("worksheet", worksheet);
        request.put("sql", sql);

        String result = "";
        String runner = "";
        CloseableHttpResponse response = null;
        try {
            // 创建Http Post请求
            HttpPost httpPost = new HttpPost(url+executeURI);
            httpPost.addHeader("token", token);
            httpPost.addHeader("company", companyId);
            httpPost.addHeader("role", roleId);
            // 创建请求内容
            StringEntity entity = new StringEntity(request.toString(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            // 执行http请求
            response = httpClient.execute(httpPost);
            String resultString = EntityUtils.toString(response.getEntity(), "utf-8");
            JSONObject resultJson = JSONObject.parseObject(new String(resultString));
            runner = resultJson.getJSONObject("data").getString("runner");
            //循环刷新查询结果
            for(;;) {
                Thread.currentThread().sleep(1000);
                String queryId = resultJson.getJSONObject("data").getJSONArray("queries").getJSONObject(0).getString("queryId");
                //状态2为查询成功，输出结果
                if (executeStatusFlag(runner).equals("2")) {
                    result = getExecuteResult(queryId);
                    return result;
                    //状态3为执行失败，输出异常
                } else if (executeStatusFlag(runner).equals("3")) {
                    result = getExecuteError(runner);
                    return result;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            response.close();
        }
        return result;
    }

    /**
     * 执行put语句，获取查询路径并且上传文件
     * @param sql
     * @return
     * @throws Exception
     */
    public static String executeAndUploadFile(String sql) throws Exception {
        // 创建Httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        JSONObject request = new JSONObject();
        request.put("catalog", database);
        request.put("schema", schema);
        request.put("worksheet", worksheet);
        request.put("sql", sql);

        String result = "";
        String runner = "";
        CloseableHttpResponse response = null;
        try {
            // 创建Http Post请求
            HttpPost httpPost = new HttpPost(url+executeURI);
            httpPost.addHeader("token", token);
            httpPost.addHeader("company", companyId);
            httpPost.addHeader("role", roleId);
            // 创建请求内容
            StringEntity entity = new StringEntity(request.toString(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            // 执行http请求
            response = httpClient.execute(httpPost);
            String resultString = EntityUtils.toString(response.getEntity(), "utf-8");
            JSONObject resultJson = JSONObject.parseObject(new String(resultString));
            runner = resultJson.getJSONObject("data").getString("runner");
            String targetPath = resultJson.getJSONObject("data").getString("paths");
            //循环刷新查询结果
            for(;;) {
                Thread.currentThread().sleep(1000);
                String queryId = resultJson.getJSONObject("data").getJSONArray("queries").getJSONObject(0).getString("queryId");
                //状态2为查询成功，输出结果
                if (executeStatusFlag(runner).equals("2")) {
                    String filePath = StringUtil.getStringBetweenSingleQuotes(sql);
                    String str1 = filePath.substring(0, 6);
                    String fileName = filePath.substring(str1.length()+1, filePath.length());

                    // 创建httpget.
                    HttpPost httppost = new HttpPost(url + uploadURI);

                    //setConnectTimeout：设置连接超时时间，单位毫秒。setConnectionRequestTimeout：设置从connect Manager获取Connection 超时时间，单位毫秒
                    RequestConfig defaultRequestConfig = RequestConfig.custom().setConnectTimeout(5000).setConnectionRequestTimeout(5000).setSocketTimeout(15000).build();
                    httppost.setConfig(defaultRequestConfig);

                    System.out.println("executing request " + httppost.getURI());

                    MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
                    multipartEntityBuilder.addBinaryBody("name=\"files\"; filename=\"123.pdf\"", new File(filePath));//添加文件
                    multipartEntityBuilder.addTextBody("path", targetPath);  //添加文本类型参数
                    HttpEntity reqEntity = multipartEntityBuilder.build();
                    httppost.setEntity(reqEntity);

                    CloseableHttpResponse uploadResponse = httpClient.execute(httppost);

                    try {
                        // 获取响应实体
                        HttpEntity uploadEntity = uploadResponse.getEntity();
                        // 打印响应状态
                        if (entity != null) {
                            result = "success";
                        }
                    } finally {
                        response.close();

                    }
                    return result;
                    //状态3为执行失败，输出异常
                } else if (executeStatusFlag(runner).equals("3")) {
                    result = getExecuteError(runner);
                    return result;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            response.close();
        }
        return result;
    }

    /**
     * 根据runnerId获取sql执行状态，1-执行中，2-成功，3-异常
     * @param runner
     * @return
     * @throws Exception
     */
    public static String executeStatusFlag(String runner) throws Exception {
        // 创建Httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();

        String status;
        CloseableHttpResponse response = null;
        try {

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("runner", runner));
            params.add(new BasicNameValuePair("worksheet", worksheet));

            // 创建Http Post请求
            URI uri = new URIBuilder().setScheme("http").setHost(host).setPort(10110).setPath(executeURI).setParameters(params).build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("token", token);
            httpGet.addHeader("company", companyId);
            httpGet.addHeader("role", roleId);

            // 执行http请求
            response = httpClient.execute(httpGet);
            String resultString = EntityUtils.toString(response.getEntity(), "utf-8");
            JSONObject resultJson = JSONObject.parseObject(new String(resultString));
            status = resultJson.getJSONObject("data").getString("status");
            return status;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            response.close();
        }
        return "1";
    }

    /**
     * 获取查询异常信息
     * @param runner
     * @return
     * @throws Exception
     */
    public static String executeStatus(String runner) throws Exception {
        // 创建Httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();

        String status;
        CloseableHttpResponse response = null;
        try {

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("runner", runner));
            params.add(new BasicNameValuePair("worksheet", worksheet));

            // 创建Http Post请求
            URI uri = new URIBuilder().setScheme("http").setHost(host).setPort(10110).setPath(executeURI).setParameters(params).build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("token", token);
            httpGet.addHeader("company", companyId);
            httpGet.addHeader("role", roleId);

            // 执行http请求
            response = httpClient.execute(httpGet);
            String resultString = EntityUtils.toString(response.getEntity(), "utf-8");
            JSONObject resultJson = JSONObject.parseObject(new String(resultString));
            status = resultJson.getJSONObject("data").getString("status");
            if (status.equals("2")) {
                String queryId = resultJson.getJSONObject("data").getJSONArray("queries").getJSONObject(0).getString("queryId");
                return queryId;
            } else {
                return resultJson.getJSONObject("data").getString("error");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            response.close();
        }
        return "";
    }

    /**
     * 获取执行状态
     * @param queryId
     * @return
     * @throws Exception
     */
    public static String getExecuteResult(String queryId) throws Exception {
        // 创建Httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();

        String list = "";
        CloseableHttpResponse response = null;
        try {

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("worksheet", worksheet));
            params.add(new BasicNameValuePair("query", queryId));

            // 创建Http Post请求
            URI uri = new URIBuilder().setScheme("http").setHost(host).setPort(10110).setPath(resultURI).setParameters(params).build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("token", token);
            httpGet.addHeader("company", companyId);
            httpGet.addHeader("role", roleId);

            // 执行http请求
            response = httpClient.execute(httpGet);
            String resultString = EntityUtils.toString(response.getEntity(), "utf-8");
            JSONObject resultJson = JSONObject.parseObject(new String(resultString));
            list = resultJson.getJSONObject("data").getString("list");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    /**
     * 获取上传路径
     * @param queryId
     * @return
     * @throws Exception
     */
    public static String getUploadPath(String queryId) throws Exception {
        // 创建Httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();

        String list = "";
        CloseableHttpResponse response = null;
        try {

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("worksheet", worksheet));
            params.add(new BasicNameValuePair("query", queryId));

            // 创建Http Post请求
            URI uri = new URIBuilder().setScheme("http").setHost(host).setPort(10110).setPath(resultURI).setParameters(params).build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("token", token);
            httpGet.addHeader("company", companyId);
            httpGet.addHeader("role", roleId);

            // 执行http请求
            response = httpClient.execute(httpGet);
            String resultString = EntityUtils.toString(response.getEntity(), "utf-8");
            JSONObject resultJson = JSONObject.parseObject(new String(resultString));
            list = resultJson.getJSONObject("data").getString("paths");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    /**
     * 获取报错信息
     * @param runner
     * @return
     * @throws Exception
     */
    public static String getExecuteError(String runner) throws Exception {
        // 创建Httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();

        String error;
        CloseableHttpResponse response = null;
        try {

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("runner", runner));
            params.add(new BasicNameValuePair("worksheet", worksheet));

            // 创建Http Post请求
            URI uri = new URIBuilder().setScheme("http").setHost(host).setPort(10110).setPath(executeURI).setParameters(params).build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("token", token);
            httpGet.addHeader("company", companyId);
            httpGet.addHeader("role", roleId);

            // 执行http请求
            response = httpClient.execute(httpGet);
            String resultString = EntityUtils.toString(response.getEntity(), "utf-8");
            JSONObject resultJson = JSONObject.parseObject(new String(resultString));
            error = resultJson.getJSONObject("data").getString("error");
            return error;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            response.close();
        }
        return "1";
    }

}
