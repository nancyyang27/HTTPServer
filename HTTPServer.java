import java.io.*;
import java.net.*;
import java.util.*;

public class HTTPServer {
    public static void main(String[] args) throws Exception {
        int port = 80; // the port number to listen on
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            Socket clientSocket = serverSocket.accept();

            // set up input and output streams for the client socket
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();
            
            // read the HTTP request line
            String requestLine = in.readLine();
            
            // parse the request line into method, URI, and HTTP version
            String[] parts = requestLine.split(" ");
            String method = parts[0];
            String uri = parts[1];
            String httpVersion = parts[2];
       
            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                String[] headerParts = line.split(": ");
                headers.put(headerParts[0], headerParts[1]);
            }

            File file = new File(uri.substring(1));
            String statusLine = "";
            String contentTypeHeader = "";
            String contentLengthHeader = "";
            
            if (!file.exists() && !method.equals("PUT")) {
                fileNotExist(file, out, httpVersion);
                continue;
            } else if (!file.isFile() && !method.equals("PUT")){
                fileIsNotFile(file, out, httpVersion);
                continue;
            } else if (method.equals("GET")) {
                // send the HTTP response
                statusLine = httpVersion + " 200 OK\r\n";
                if (uri.endsWith(".txt")) {
                    contentTypeHeader = "Content-Type: text/plain\r\n";
                } else if (uri.endsWith(".png")) {
                    contentTypeHeader = "Content-Type: image/png\r\n";
                }
                else {
                    System.out.println("Unsupported file type");
                    continue;
                } 
                contentLengthHeader = "Content-Length: " + file.length() + "\r\n";
                try {
                    out.write(statusLine.getBytes());
                    out.write(contentTypeHeader.getBytes());
                    out.write(contentLengthHeader.getBytes());
                    out.write("\r\n".getBytes());
                    
                    // System.out.println(statusLine);
                    // System.out.println(contentTypeHeader);
                    // System.out.println(contentLengthHeader);
                    // System.out.println("\r\n");

                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                } catch (Exception error) {
                    error.printStackTrace();
                }
            } else if (method.equals("POST") && uri.endsWith(".txt")) {
                StringBuilder bodyBuilder = new StringBuilder();
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                String body = "";
                for (int i = 0; i < contentLength; i++) {
                    bodyBuilder.append((char) in.read());
                }
                body = bodyBuilder.toString();
                
                // append the body to the resource
                FileWriter writer = new FileWriter(uri.substring(1), true);
                writer.write(body);
                writer.close();
                
                // send the HTTP response
                statusLine = httpVersion + " 200 OK\r\n";
                contentTypeHeader = "Content-Type: text/plain\r\n";
                contentLengthHeader = "Content-Length: 0\r\n";
                try {
                    out.write(statusLine.getBytes());
                    out.write(contentTypeHeader.getBytes());
                    out.write(contentLengthHeader.getBytes());
                    out.write("\r\n".getBytes());
                    
                    // System.out.println(statusLine);
                    // System.out.println(contentTypeHeader);
                    // System.out.println(contentLengthHeader);
                    // System.out.println("\r\n");
                } catch (Exception error) {
                    error.printStackTrace();
                }
            } else if (method.equals("PUT")) {
                boolean fileExist = true;
                if (!file.exists()) {
                    // If not, create a new file
                    file.createNewFile();
                    fileExist = false;
                }
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                for (int i = 0; i < contentLength; i++) {
                    fileOutputStream.write(in.read());
                }
                fileOutputStream.close();
                if (!fileExist) {
                    statusLine = httpVersion + " 201 Created\r\n";
                } else {
                    statusLine = httpVersion + " 200 OK\r\n";
                }
                
                contentTypeHeader = "Content-Type: text/plain\r\n";
                contentLengthHeader = "Content-Length: 0\r\n";
                try {
                    out.write(statusLine.getBytes());
                    out.write(contentTypeHeader.getBytes());
                    out.write(contentLengthHeader.getBytes());
                    out.write("\r\n".getBytes());
                    
                    // System.out.println(statusLine);
                    // System.out.println(contentTypeHeader);
                    // System.out.println(contentLengthHeader);
                    // System.out.println("\r\n");
                } catch (Exception error) {
                    error.printStackTrace();
                }
            } else if (method.equals("DELETE")) {
                boolean success = file.delete();
                if (!success) {
                    // delete failed
                    statusLine = httpVersion + " 500 Internal Server Error\r\n";
                    contentLengthHeader = "Content-Length: 0\r\n";
                } else {
                    // send the HTTP response
                    statusLine = httpVersion + " 200 OK\r\n";
                    contentLengthHeader = "Content-Length: 0\r\n";
                }
                try {
                    out.write(statusLine.getBytes());
                    out.write(contentLengthHeader.getBytes());
                    out.write("\r\n".getBytes());
                    
                    // System.out.println(statusLine);
                    // System.out.println(contentLengthHeader);
                    // System.out.println("\r\n");
                } catch (Exception error) {
                    error.printStackTrace();
                }
            } else if (method.equals("OPTION")) {
                // for put, if the file not exist, create a file
                // otherwise append to the existing file
                // so always allow
                String allowedMethods = "OPTIONS, GET, PUT";

                if (file.exists()) {
                    allowedMethods += ", DELETE"
                    String fileName = resourceFile.getName();
                    if (fileName.endsWith(".txt")) {
                        allowedMethods += ", POST";
                    } 
                } 


                String statusLine = "HTTP/1.1 200 OK\r\n";
                String allowHeader = "Allow: " + allowedMethods + "\r\n";
                String response = statusLine + allowHeader + "\r\n";

                out.write(response.getBytes());
            } else if (method.equals("HEADER")) {
                String contentType = Files.probeContentType(resourceFile.toPath());
                long contentLength = resourceFile.length();

                // Construct the response headers
                String statusLine = "HTTP/1.1 200 OK\r\n";
                String contentTypeHeader = "Content-Type: " + contentType + "\r\n";
                String contentLengthHeader = "Content-Length: " + contentLength + "\r\n";
                String response = statusLine + contentTypeHeader + contentLengthHeader + "\r\n";

                // Send the response headers
                out.write(response.getBytes());
                
            }
            
        }
    }

    public static void fileNotExist(File file, OutputStream out, String httpVersion) {
        String statusLine = httpVersion + " 404 Not Found\r\n";
        String contentLengthHeader = "Content-Length: 0\r\n";
        try {
            out.write(statusLine.getBytes());
            out.write(contentLengthHeader.getBytes());
            out.write("\r\n".getBytes());
            // System.out.println(statusLine);
            // System.out.println(contentLengthHeader);
            // System.out.println("\r\n");
        } catch (IOException e) {
            // handle the exception here
            e.printStackTrace();
        }
        
    }
    
    public static void fileIsNotFile(File file, OutputStream out, String httpVersion) {
        String statusLine = httpVersion + " 500 Internal Server Error\r\n";
        String contentLengthHeader = "Content-Length: 0\r\n";
        try {
            out.write(statusLine.getBytes());
            out.write(contentLengthHeader.getBytes());
            out.write("\r\n".getBytes());
        } catch (IOException e) {
            // handle the exception here
            e.printStackTrace();
        }
    }
}