import org.apache.commons.codec.binary.StringUtils;
import utils.SpineApi;
import utils.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SpineClient {

    public static void main(String[] args) throws IOException {

        System.out.println("\n Welcome useing spineCLI! Please input \"login\" to login at first \n");
        for (;;) {
            BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));
            String str = buf.readLine();
            switch (str) {
                //用户登录，获取token,role,company，自动创建工作区
                case "login":
                    System.out.println("email:");
                    String email = buf.readLine();
                    System.out.println("password:");
                    String pwd = buf.readLine();
                    try {
                        SpineApi.login(email, pwd);
                    } catch (Exception e) {
                        System.out.println("Login fail！Please check your email and password");
                    }
                    try {
                        SpineApi.getWorksheet();
                    } catch (Exception e) {
                        System.out.println("worksheets apply fail！");
                    }
                    System.out.println("\n ----------------Input \"execute\" to input SQL you want to execute----------------\n");
                    break;
                //创建worksheet
                case "worksheet":
                    try {
                        SpineApi.getWorksheet();
                    } catch (Exception e) {
                        System.out.println("worksheets apply fail！");
                    }
                    System.out.println("\n ----------------Input \"execute\" to input SQL you want to execute---------------- \n");
                    break;
                //修改database和schema
                case "config":
                    System.out.println("\n database: ");
                    SpineApi.database = buf.readLine();
                    System.out.println("\n schema: ");
                    SpineApi.schema = buf.readLine();
                    break;
                //执行sql语句并直接获取结果
                case "execute":
                    for (;;) {
                        if (SpineApi.database.equals("")) {
                            System.out.println("\n SQL > ");
                        } else {
                            System.out.println("\n " + SpineApi.database +" > ");
                        }
                        //读取输入的sql语句
                        String sql = buf.readLine();
                        //按照空格分隔
                        String arr[] = sql.split("\\s+");
                        //提取关键字
                        String keyword = arr[0];
                        //用户输入exit则退出exeute界面
                        if (sql.toUpperCase().equals("EXIT")) {
                            break;
                        }
                        //检测关键词use，切换database和schema
                        if (keyword.toUpperCase().equals("USE")) {
                            System.out.println("\n-------------Executing, please wait for result------------\n");
                            //获取database\schema，去除分号
                            String source = StringUtil.deleteString(arr[1], ';');
                            if (source.contains(".")) {
                                int index = source.indexOf(".");
                                int length = source.length();
//                                String datasource[] = sql.split(".");
                                SpineApi.database = source.substring(0, index);
                                SpineApi.schema = source.substring(index + 1, length);
                                continue;
                            } else {
                                SpineApi.database = source;
                                continue;
                            }
                        }
                        String directResult = "";
                        //检测关键词put，上传文件操作 todo
                        if(keyword.toUpperCase().equals("PUT")) {
                            try {
                                System.out.println("\n-------------Uploading, please wait for result------------\n");
                                directResult = SpineApi.executeAndUploadFile(sql);
                                continue;
                            } catch (Exception e) {
                                System.out.println("upload fail！");
                            }
                        }
                        try {
                            System.out.println("\n-------------Executing, please wait for result------------\n");
                            directResult = SpineApi.executeAndGetResult(sql);
                        } catch (Exception e) {
                            System.out.println("worksheets apply fail！");
                        } finally {
                            System.out.println(directResult);
                            System.out.println("\n---------------------Executing ends---------------------\n");
                        }
                    }
                    break;
                //执行sql语句获取此次查询的runnerId
                case "runner":
                    if (SpineApi.database.equals("")) {
                        System.out.println("\n SQL > ");
                    } else {
                        System.out.println("\n " + SpineApi.database +" > ");
                    }

                    String sql_1 = buf.readLine();
                    String runner = "";
                    try {
                        runner = SpineApi.execute(sql_1);
                    } catch (Exception e) {
                        System.out.println("worksheets apply fail！");
                    } finally {
                        System.out.println("\n runner id is " + runner + ", input \"status\" to look up status of query \n");
                    }
                    break;
                //根据runnerId获取查询状态和queryId
                case "status":
                    System.out.println("\n runner id : ");
                    String query = "";
                    String inputRunner = buf.readLine();
                    try {
                        query = SpineApi.executeStatus(inputRunner);
                    } catch (Exception e) {
                        System.out.println("worksheets apply fail！");
                    } finally {
                        System.out.println("query id is " + query + ", input \"result\" to look up result \n");
                    }
                    break;
                //根据queryId获取查询结果
                case "result":
                    System.out.println("\n query id : ");
                    String result = "";
                    String inputQueryId = buf.readLine();
                    try {
                        result = SpineApi.getExecuteResult(inputQueryId);
                    } catch (Exception e) {
                        System.out.println("error");
                    } finally {
                        System.out.println(result);
                        System.out.println("\nExecution ends\n");
                    }
                    break;
                case "exit":
                    System.exit(0);
                default:
                    System.out.println("Invalid input");
            }
        }




    }


}
