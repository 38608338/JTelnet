package leo;

import java.net.*; 
import java.awt.*; 
import java.io.*; 
//import java.lang.*; 
import java.applet.*;

public class Telnet extends Applet 
{ 
	private static final long serialVersionUID = -1042801863243705845L;
	//属性
        String hoststr, portstr;        // 给定主机和端口参数
        Panel controls; 
        TelnetClient tn;                // telnet component 
        TextField hostfield; 
        TextField portfield; 
        Button connect, disconnect; 
  	//初始化
        public void init() 
        { 
        	//创建接口
        	setLayout(new BorderLayout()); 
        	add("Center",tn = new TelnetClient()); 
        	add("North",controls = new Panel()); 
        	controls.setLayout(new FlowLayout()); 
        	controls.add(new Label("Host")); 
        	controls.add(hostfield = new TextField()); 
        	controls.add(new Label("Port")); 
        	controls.add(portfield = new TextField(5)); 
        	controls.add(connect = new Button("Connect")); 
        	controls.add(disconnect = new Button("Disconnect")); 
  
        	setSize(640, 480);
        	
        	// 获取参数 
        	String hoststr = getParameter("host"); 
        	String portstr = getParameter("port"); 
        	if (hoststr == null) hoststr = "127.0.0.1"; 
        	if (portstr == null) portstr = "23"; 
        	hostfield.setText(hoststr); 
        	portfield.setText(portstr); 
        } 
  	//启动线程	
        public void start() 
        { 
        	if (!hostfield.getText().isEmpty() && !portfield.getText().isEmpty()) {
            	tn.disconnect(); 
            	tn.connect(hostfield.getText(), Integer.parseInt(portfield.getText())); 
			}
        } 
  	//线程停止
        public void stop() 
        { 
        	tn.disconnect(); 
        } 
 	//线程动作 
        public boolean action(Event e, Object arg) 
        { 
        	if (e.target == connect) 
                start(); 
        	else if (e.target == disconnect) 
                stop(); 
        	return true; 
        } 
} 
  
// TelnetClient 
// 连接指定主机和端口 
class TelnetClient extends Canvas implements Runnable 
{ 
	private static final long serialVersionUID = -1369844874234724552L;
		//属性
        boolean debug = false; 		//debug标记
        String host;                    // 远端主机 
        int port;                       // 端口 
        Socket s;                       // 连接 
        InputStream in;                 // 输入流
        OutputStream out;               // 输出流 
        Thread th;                      // 读入数据线程
        Font fn;                        // 当前字体 
        Image back;                     // 背景图片 
        int x, y;                       // 鼠标参数 
        int chw,chh;                    // 字符大小 
        int chd;                        // 字符偏置 
        int width,height;               // Applet大小 
        int w,h;                        // Applet大小 (in chars) 
        char ch[][];                    // 显示的字符数组 
        Graphics gr,bgr;                // 前后图片  
        String term = "dumb";           
        boolean echo;                   // 响应键 
  
        // reshape 
        // 绘制Telnet客户端组件大小 
        public void reshape(int nx, int ny, int nw, int nh) 
        { 
        	if (nw != width || nh != height) 
        	{ 
                	width = nw; 
                	height = nh;  
                	// 设置字体 
                	gr = getGraphics(); 
                	gr.setColor(Color.black); 
                	
                	GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
                	 String[] str = environment.getAvailableFontFamilyNames();// 获取系统字体
                	 for (String fontname : str) {
                		 if (debug)System.out.println(fontname);
					}
                	
                	fn = new Font("Microsoft Sans Serif",Font.PLAIN,10); 
                	if (fn != null) gr.setFont(fn); 
                	FontMetrics fnm = gr.getFontMetrics(); 
                	chw = fnm.getMaxAdvance() /2; 
                	chh = fnm.getHeight(); 
                	chd = fnm.getDescent();   
                	// 规划出绘制区域
                	h = nh / chh; 
                	w = nw / chw; 
                	ch = new char [w][h];  
                	// 构造背景图片 
                	back = createImage(width, height); 
                	bgr = back.getGraphics(); 
                	bgr.setFont(fn); 
                	bgr.setColor(Color.black); 
                	clearch(); 
                } 
        	super.reshape(nx,ny,nw,nh); 
        } 
  
        // connect 
        //连接主机 
        void connect(String givenhost, int givenport) 
        { 
        	host = givenhost; 
        	port = givenport;  
        	if (debug) System.out.println("Height = "+String.valueOf(h)); 
        	if (debug) System.out.println("Width  = "+String.valueOf(w));   
        	// 重新显示 
        	clearch(); 
        	echo = true; 
        	requestFocus();  
        	// 打开连接 
        	try 
        	{ 
                	try 
                	{ 
                        	if ((s = new Socket(host,port)) == null) 
                        	{ 
                                	display("Failed to connect to host "+host+"\n"); 
                                	return; 
                                } 
                        } 
                	catch(UnknownHostException e) 
                	{ 
                        	display("Host " + host + " not found\n"); 
                        	return; 
                        } 
                } 
        	catch(IOException e) 
        	{ 
                	display("Failed to connect to host "+host+"\n"); 
                	return; 
                } 
        	try 
        	{ 
                	in = s.getInputStream(); 
                	out = s.getOutputStream(); 
                } 
        	catch(IOException e) 
        	{ 
                	if (debug) System.out.println("Failed to get stream from socket"); 
                	System.exit(5); 
                } 
        	display("Connected to "+host+"\n"); 
        	if (debug) System.out.println("Connected to host");   
        	// 读入数据
        	th = new Thread(this); 
        	th.start(); 
        } 
  
        // disconnect 
        // 关闭连接 
        void disconnect() 
        { 
        	if (th != null) 
        	{ 
                	display("\nDisconnected from "+host+"\n"); 
                	th.stop(); 
                	th = null; 
                	s = null; in = null; out = null; 
                } 
        } 
  
        // clearch 
        // 清除字符数组重新设置游标
        void clearch() 
        { 
        	int i,j; 
        	for(i=0; i<w; i++) 
                for(j=0; j<h; j++) 
                        ch[i][j] = ' '; 
        	x = y = 0; 
        	bgr.setColor(Color.white); 
        	bgr.fillRect(0, 0, width, height); 
        	paint(gr); 
        } 
  
        // keyDown 
        // 点击按钮事件 
        public boolean keyDown(Event e, int k) 
        { 
        	if (out != null) 
        	{ 
                	int kp = e.key; 
                	if (debug) System.out.println("Pressed key " + String.valueOf(kp)); 
                	transmitch((char)kp); 
                	if (echo) 
                	{ 
                        	if (debug) System.out.println("Echo'd "+String.valueOf(kp)); 
                        	displaych((char)kp); 
                        } 
                } 
        	return true; 
        } 
  
        // mouseDown 
        // 按动鼠标获得焦点
        public boolean mouseDown(Event e, int x, int y) 
        { 
        	requestFocus(); 
        	return true; 
        } 
  
        // paint 
        // 绘制图片 
        public void paint(Graphics g) 
        { 
        	gr.drawImage(back,0,0,this); 
        } 
  
        // renderchar 
        // 阅读字符
        void renderchar(char c, int x, int y, boolean back) 
        { 
        	gr.setColor(Color.white); 
        	gr.fillRect(x*chw, y*chh, chw, chh); 
        	gr.setColor(Color.black); 
        	gr.drawString(String.valueOf(c), x*chw, (y+1)*chh-chd);   
        	if (back) 
        	{ 
                	bgr.setColor(Color.white); 
                	bgr.fillRect(x*chw, y*chh, chw, chh); 
                	bgr.setColor(Color.black); 
                	bgr.drawString(String.valueOf(c), x*chw, (y+1)*chh-chd); 
                } 
        } 
  
        // run 
        // 阅读和显示字符 
        public void run() 
        { 
        	while(true)  displaych(readch()); 
        } 
  
        // readch 
        // 从另一个终端读入字符 
        char readch() 
        { 
        	int c = 0; 
        	try {        c = in.read(); } 
        	catch(IOException e) 
        	{        shutdown();} 
        	if (c == -1) shutdown(); 
        	if (debug) System.out.println("Got char "+String.valueOf(c)+" = "+String.valueOf((char)c)); 
        	return (char)c; 
        } 
  
        // shutdown 
        // 中断连接 
        void shutdown() 
        { 
        	display("\nConnection closed\n"); 
        	s = null; in = null; out = null; 
        	Thread.currentThread().stop(); 
        } 
  
        // display 
        // 在Telnet窗口显示字符串 
        void display(String str) 
        { 
        	int i; 
        	for(i=0; i<str.length(); i++) 
                	displaych(str.charAt(i)); 
        } 
  
        // displaych 
        // 在当前鼠标位置显示一个字符 
        void displaych(char c) 
        { 
        	if (c == '\n') 
        	{ 
                	// 新行 
                	renderchar(ch[x][y], x, y, false);      // erase cursor 
                	x = 0; 
                	if (y == h-1) 
                	{ 
                        	gr.copyArea(0, chh, w*chw, (h-1)*chh, 0, -chh); 
                        	gr.setColor(Color.white); 
                        	gr.fillRect(0, (h-1)*chh, width, chh); 
                        	bgr.copyArea(0, chh, w*chw, (h-1)*chh, 0, -chh); 
                        	bgr.setColor(Color.white); 
                        	bgr.fillRect(0, (h-1)*chh, width, chh); 
                        	int i,j; 
                        	for(i=0; i<w; i++) 
                        	{ 
                                	for(j=0; j<h-1; j++) ch[i][j] = ch[i][j+1]; 
                                	ch[i][h-1] = ' '; 
                                } 
                        } 
                	else       y++; 
                } 
        	else if (c == '\t') 
        	{      //Tab           	
                	int i; 
                	for(i=8; i>x%8; i--) displaych(' '); 
                } 
        	else if (c == (char)8) 
        	{   // Backspace 
                	renderchar(ch[x][y], x, y, false);      // erase cursor 
                	if (x != 0) x--; 
                } 
        	else if (c >= 32 && c < 127) 
        	{  // Some printable character 
                	renderchar(c, x, y, true); 
                	ch[x][y] = c; 
                	if (x == w-1) displaych('\n'); 
                	else    x++; 
                } 
        	else if (c == 255) 
        	{ 
                	// Telnet IAC 
                	char cmd = readch(); 
                	char opt = readch();  
                	switch(opt) 
                	{ 
                        case 1:         // echo 
                        	if (cmd == 251) echo = false; 
                        	else if (cmd == 252) echo = true; 
                        	break; 
  
                        case 3:         // supress go-ahead 
                        	break; 
  
                        case 24:        // terminal type 
                        	if (cmd == 253) 
                        	{ 
                                	// IAC WILL terminal-type 
                                	transmitch((char)255); 
                                	transmitch((char)251); 
                                	transmitch((char)24); 
                                	// IAC SB terminal-type IS <term> IAC SE 
                                	transmitch((char)255); 
                                	transmitch((char)250); 
                                	transmitch((char)24); 
                                	transmitch((char)0); 
                                	transmit(term); 
                                	transmitch((char)255); 
                                	transmitch((char)240); 
                                } 
                        	else if (cmd == 250) 
                        	{ 
                                	while(readch() != 240); 
                                } 
                        	break; 
  
                        default:        // some other command 
                        	if (cmd == 253) 
                        	{ 
                                	// IAC DONT whatever 
                                	transmitch((char)255); 
                                	transmitch((char)252); 
                                	transmitch((char)opt); 
                                } 
                       	 	break; 
                        } 
                } 
        	renderchar('_', x, y, false);   // draw cursor 
        } 
  
        void transmit(String str) 
        { 
        	int i;  
        	for(i=0; i<str.length(); i++)  transmitch(str.charAt(i)); 
        } 
  
        void transmitch(char c) 
        { 
        	if (c == '\n') transmitch('\r'); 
        	try 
        	{ 
                	out.write((int)c); 
                	out.flush(); 
                } 
        	catch(IOException e){ };   
        	if (debug) 
        	System.out.println("Sent char " + String.valueOf((int)c) + " = " + String.valueOf(c)); 
        } 
}
