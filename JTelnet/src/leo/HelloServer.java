package leo;

import java.io.BufferedReader;  
import java.io.IOException;  
import java.io.InputStream;  
import java.io.InputStreamReader;  
import java.io.OutputStream;  
import java.net.ServerSocket;  
import java.net.Socket;  
import java.text.SimpleDateFormat;  
import java.util.Date;  
import java.util.concurrent.ExecutorService;  
import java.util.concurrent.Executors;  
  
public class HelloServer {  
  
    public static final int SERVER_PORT = 23;  
    private ServerSocket serverSocket = null;  
    private ExecutorService executorService = null;  
    private final int POOL_SIZE = 2;  
      
    public HelloServer() throws Exception{  
        int cpuCount = Runtime.getRuntime().availableProcessors();  
        executorService = Executors.newFixedThreadPool(cpuCount * POOL_SIZE);  
        serverSocket = new ServerSocket(SERVER_PORT);  
        System.out.println("������������,������...");  
        while(true){  
            try{  
                Socket socket = serverSocket.accept();  
                executorService.execute(new HelloResponser(socket));  
            }catch(Exception e){  
                //e.printStackTrace();
            	break;
            }
        }
        executorService.shutdown();
        //executorService.shutdownNow();
        //System.exit(0);
        //Runtime.getRuntime().exit(0);
    }  
      
    //  
    class HelloResponser implements Runnable{  
        private Socket socket = null;  
          
        public HelloResponser(Socket socket){  
            this.socket = socket;  
        }  
          
        @Override  
        public void run() {  
            try{  
                //  
                String clientIp = socket.getInetAddress().getHostAddress();  
                System.out.println("��ʼ�������� " + clientIp + ":" + socket.getPort() + " ������");  
                InputStream socketInStream = socket.getInputStream();  
                BufferedReader br = new BufferedReader(new InputStreamReader(socketInStream, "UTF-8"));  
                //BufferedReader br = new BufferedReader(new InputStreamReader(socketInStream));
                  
                //  
                OutputStream socketOutStream = socket.getOutputStream();  
                String clientRequestString = null;  
                while((clientRequestString = br.readLine()) != null){  
                    System.out.println("���յ��ͻ��� " + clientIp + " ����Ϣ:" + clientRequestString);  
                    String serverReturn = null;  
                    if(clientRequestString.equals("sb")){  
                        serverReturn = "��Ҳ��SB.\r\n";  
                        System.out.println("���͸��ͻ���" + clientIp + "Ӧ����Ϣ:" + serverReturn);  
                        socketOutStream.write(serverReturn.getBytes());  
                        System.out.println("�������� " + clientIp + ":" + socket.getPort() + " ������");  
                        break;  
                    }
                    else if(clientRequestString.equals("SHUTDOWN")){  
                        serverReturn = "THE SERVER HAS SHUTDOWN.\r\n";  
                        System.out.println("���͸��ͻ���" + clientIp + "Ӧ����Ϣ:" + serverReturn);  
                        socketOutStream.write(serverReturn.getBytes());  
                        System.out.println("�������� " + clientIp + ":" + socket.getPort() + " ������"); 
                        socket.close();
                        serverSocket.close();
                        System.out.println("��������ֹͣ");  
                        break;  
                    }
                    else{  
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E");  
                        serverReturn = df.format(new Date()) +" reply you:"+ clientRequestString+ " has done!\r\n";  
                        System.out.println("���͸��ͻ��� " + clientIp + "Ӧ����Ϣ:" + serverReturn);  
                        socketOutStream.write(serverReturn.getBytes());  
                    }  
                }  
            }catch(Exception e){  
                e.printStackTrace();  
            }finally{  
                    try {  
                        if(socket != null){  
                            socket.close();  
                        }  
                    } catch (IOException e) {  
                        e.printStackTrace();  
                    }  
                }  
            }  
    }  
      
    //  
    public static void main(String[] args) throws Exception {  
        new HelloServer();  
    }  
}  
