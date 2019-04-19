import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Reactor 接收连接，直接负责 I/O 的读写，拿到字节后有单线程和多线程两种处理器：
 * <p>
 * 单线程处理器：业务处理也由 Reactor 线程来做
 * <br>
 * 多线程处理器：业务处理由线程池线程来做
 *
 * @author tongwu.net
 * @see BasicHandler
 */
public class Reactor implements Runnable {

  public static void main(String[] args) {
    try {
      Thread th = new Thread(new Reactor(10393));
      th.setName("Reactor");
      th.start();
      th.join();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 选择器，通知通道就绪的事件
   */
  final Selector selector;
  final ServerSocketChannel serverSocket;

  public Reactor(int port) throws IOException {
    selector = Selector.open();
    serverSocket = ServerSocketChannel.open();
    // 绑定端口
    serverSocket.socket().bind(new InetSocketAddress(port));
    // 设置成非阻塞模式
    serverSocket.configureBlocking(false);
    // 注册并关注一个 IO 事件
    SelectionKey sk = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
    // 关联事件的处理程序
    sk.attach(new Acceptor());

    System.out.println("Listening on port " + port);
  }

  @Override
  public void run() {
    try {
      // 死循环
      while (!Thread.interrupted()) {
        // 阻塞，直到有通道事件就绪
        selector.select();
        // 拿到就绪通道 SelectionKey 的集合
        Set<SelectionKey> selected = selector.selectedKeys();
        Iterator<SelectionKey> it = selected.iterator();
        while (it.hasNext()) {
          SelectionKey skTmp = it.next();
          // 分发
          dispatch(skTmp);
        }
        // 清空就绪通道的 key
        selected.clear();
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  void dispatch(SelectionKey k) {
    // 获取key关联的处理器
    Runnable r = (Runnable) (k.attachment());
    // 执行处理
    if (r != null) {
      r.run();
    }
  }

  /**
   * 处理连接建立事件
   *
   * @author tongwu.net
   */
  class Acceptor implements Runnable {

    @Override
    public void run() {
      try {
        // 接收连接，非阻塞模式下，没有连接直接返回 null
        SocketChannel sc = serverSocket.accept();
        if (sc != null) {
          // 把提示发到界面
          sc.write(ByteBuffer.wrap(
              "Implementation of Reactor Design Partten by tonwu.net\r\nreactor> ".getBytes()));
          System.out.println("Accept and handler - " + sc.socket().getLocalSocketAddress());
          // 单线程处理连接
          new BasicHandler(selector, sc);
          // 线程池处理连接
          //new MultithreadHandler(selector, sc);
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }
}
