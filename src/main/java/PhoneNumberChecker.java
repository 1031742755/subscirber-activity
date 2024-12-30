import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


public class PhoneNumberChecker {
    // 号码段
    // 菲律宾
    private static String[] prefix_PH ={
            "817", "895", "896", "897", "898", "905", "906",
            "915", "916", "917", "926", "927", "935", "936",
            "937", "945", "953", "954", "955", "956", "957",
            "965", "966", "967", "975", "976", "977", "978",
            "979", "991", "992", "993", "994", "995", "996", "997"
    };
    // 泰国
    private static String[] prefix_TH ={

    };


    public static String getAccessToken(String tokenUrl) throws IOException {
        URL url = new URL(tokenUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        // 设置请求参数
        String data = "grant_type=" + URLEncoder.encode("password", "UTF-8") +
                "&client_id=" + URLEncoder.encode("ip-subscriber-activity", "UTF-8") +
                // 🌟🌟🌟注意使用的测试账号🌟🌟🌟
                "&username=" + URLEncoder.encode("china-sales", "UTF-8") +
                "&password=" + URLEncoder.encode("TmZhE6bDazr5bjTM6CfZ", "UTF-8");

        // 发送请求数据
        try (OutputStream os = conn.getOutputStream()) {
            os.write(data.getBytes());
            os.flush();
        }

        // 获取响应代码
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // 处理响应
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            return parseToken(content.toString());
        } else {
            // 处理错误响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            throw new IOException("Failed to retrieve access token, server returned: " + responseCode);
        }
    }

    private static String parseToken(String response) {
        // 解析返回的JSON字符串并提取access_token (示例假设响应为JSON格式)
        // 你可以使用JSON库如org.json或Gson解析返回的access_token
        JSONObject jsonObject = new JSONObject(response);
        System.out.println("access_token:" + jsonObject.get("access_token"));
        return jsonObject.get("access_token").toString(); // 此处应根据具体响应格式提取token
    }

    private static JSONObject checkPhoneNumberStatus(String checkUrl, String token) throws IOException {
        try {
            URL url = new URL(checkUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("User-Agent", "PostmanRuntime/7.41.1");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            conn.setRequestProperty("Connection", "keep-alive");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            // 解析JSON获取状态 (假设返回的JSON中包含status字段)
            JSONObject jsonObject = parseData(content.toString());
            in.close();
            conn.disconnect();
            return jsonObject;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    private static JSONObject parseData(String response) {
        // 解析返回的JSON字符串并提取access_token (示例假设响应为JSON格式)
        // 你可以使用JSON库如org.json或Gson解析返回的access_token
        JSONObject jsonObject = new JSONObject(response);
        JSONObject dataObject = jsonObject.getJSONObject("data");
        return dataObject;
    }

    // 判断手机号码的前缀是否在数组中
    public static boolean isPrefixInArray(String prefix, String[] array) {
        for (String item : array) {
            if (prefix.startsWith("63"+item)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        String tokenUrl = "https://api.ipification.com/auth/realms/ipification/protocol/openid-connect/token";

        try {
            // 1. 读取Excel文件中的手机号码
            FileInputStream file = new FileInputStream("/Users/mac/Desktop/test.xlsx");
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheetAt(0);

            // 4. 获取访问令牌
            String token = getAccessToken(tokenUrl);

            // 4. 遍历每一行，获取手机号码并检测状态
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                if(i%30 == 0){
                    token = getAccessToken(tokenUrl);
                }
                Row row = sheet.getRow(i);
                if (row != null) {
                    String phoneNumber = row.getCell(0).getStringCellValue();
                    String temp = phoneNumber;
                     Cell phoneNumberCell = row.getCell(0);
                    if (!phoneNumber.isEmpty() && phoneNumber != null) {
                        // 菲律宾
                        if (phoneNumber.startsWith("0")) {
                            // 如果是，则将 "0" 替换为 "63"
                            temp = "63" + phoneNumber.substring(1);
                        }
                        // 获取手机号码的前五位
                        String phonePrefix = temp.substring(0, 5);  // 获取前5位
                        // 判断前五位是否包含在数组中
                        if (isPrefixInArray(phonePrefix, prefix_PH)) {
                            Cell phoneNumbercell = row.createCell(0);
                            phoneNumbercell.setCellValue(phoneNumber);
                        } else {
//                            System.out.println("手机号码前五位不包含在数组中: " + phonePrefix);
                            Cell deviceCell = row.createCell(1);  // 第二列，索引从0开始
                            deviceCell.setCellValue("NULL");
                            Cell statusCell = row.createCell(2);
                            statusCell.setCellValue("NULL");
                            Cell operatorCell = row.createCell(3);
                            operatorCell.setCellValue("NULL");
                            continue;
                        }
                       String checkUrl = "https://api.ipification.com/subscriber/status/v1/"+phoneNumber;  // 替换手机号码到URL
                        try {
                            JSONObject jsonObject = checkPhoneNumberStatus(checkUrl, token);
                            String device = jsonObject.optString("device_status", "null");
                            String status = jsonObject.optString("subscriber_status", "null");
                            String operator = jsonObject.optString("operator_code", "null");
//                            System.out.println(device + "-" + status + "-" + operator + " " +checkUrl);
                            // 6. 将结果写入到Excel文件的新列
                            Cell deviceCell = row.createCell(1);  // 第二列，索引从0开始
                            deviceCell.setCellValue(device);
                            Cell statusCell = row.createCell(2);
                            statusCell.setCellValue(status);
                            Cell operatorCell = row.createCell(3);
                            operatorCell.setCellValue(operator);
                        }catch (Exception e){
                            e.printStackTrace();
                            // 7. 保存结果到新的Excel文件
                            FileOutputStream outFile = new FileOutputStream("/Users/mac/Desktop/out.xlsx");
                            workbook.write(outFile);
                            workbook.close();

                            outFile.close();
                            file.close();
                            System.out.println("检测结果已保存");
                            return;
                        }
                    }
                }
            }

            // 7. 保存结果到新的Excel文件
            FileOutputStream outFile = new FileOutputStream("/Users/mac/Desktop/out.xlsx");
             workbook.write(outFile);
            workbook.close();

            outFile.close();
            file.close();
            System.out.println("检测结果已保存");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
