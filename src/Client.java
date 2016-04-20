import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

public class Client
{
    private static final int  PORT = 2444;
    private static final String HOST = "192.168.1.6";

    private JFrame frame;
    private JTextArea allMessagesArea;
    private JTextArea inputArea;
    private JButton buttonSend;
    private JButton buttonExit;
    private String login;
    private Socket socket;
    PrintWriter output;
    Scanner input;
    StringBuffer buffer;
    public  void addComponentsToPane(Container pane)
    {
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10,10,10,10);
        c.fill = GridBagConstraints.HORIZONTAL;

        allMessagesArea = new JTextArea(25,50);
        allMessagesArea.setEditable(false);
        //make auto next line
        allMessagesArea.setLineWrap(true);
        allMessagesArea.setWrapStyleWord(true);
        c.weighty = 0.6;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=0;
        c.gridy=0;
        c.gridwidth=2;
        //adding scrollbar
        JScrollPane scrollBar = new JScrollPane(allMessagesArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        DefaultCaret caret = (DefaultCaret) allMessagesArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        ContextMenuMouseListener mouseListener = new ContextMenuMouseListener();
        allMessagesArea.addMouseListener(mouseListener);

        pane.add(scrollBar, c);

        inputArea = new JTextArea(12,50)
        {
            public void addNotify() {
                super.addNotify();
                requestFocus();
            }
        };
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth=2;
        c.weighty =0.3;
        c.gridx =0;
        c.gridy =1;
        inputArea.addMouseListener(mouseListener);
        DefaultCaret caretInput = (DefaultCaret) allMessagesArea.getCaret();
        caretInput.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        pane.add(inputArea, c);



        buttonSend = new JButton("Send");
        c.weightx=0.5;
        c.weighty = 0.1;
        c.fill =  GridBagConstraints.HORIZONTAL;
        c.gridx =0;
        c.gridy=2;
        c.gridwidth =1;
        pane.add(buttonSend, c);


        buttonExit = new JButton("Exit");
        c.weightx =0.5;
        c.weighty = 0.1;
        c.fill =  GridBagConstraints.HORIZONTAL;
        c.gridx =1;
        c.gridy=2;
        c.gridwidth =1;
        pane.add(buttonExit, c);

        Thread inputListener = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    while (!Thread.currentThread().isInterrupted())
                    {
                        String text =new String(input.nextLine().getBytes( "UTF-8"),  "UTF-8");
                        buffer.append(text);
                    }
                }
                catch (Exception e)
                {

                }
            }
        });
        inputListener.start();


        Thread allMessageAreaRefreshener =  new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    while (!Thread.currentThread().isInterrupted())
                    {
                        Thread.sleep(100);
                        synchronized (buffer)
                        {
                            if(!buffer.toString().isEmpty())
                            {
                                allMessagesArea.append("\n" + buffer.toString() + "\n");
                                buffer.delete(0, buffer.length());
                            }
                        }
                    }
                }
                catch (Exception e)
                {

                }
            }
        });
        allMessageAreaRefreshener.start();

        inputArea.addKeyListener(new KeyListener()
        {
            @Override
            public void keyTyped(KeyEvent e)
            {
                //do nothing
            }

            @Override
            public void keyPressed(KeyEvent e)
            {
                if (((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)&&(e.getKeyCode() == KeyEvent.VK_ENTER))
                    if(!inputArea.getText().isEmpty())
                    {
                        output.println(inputArea.getText().replaceAll("\\s"," ").trim());
                        inputArea.setText("");
                        inputArea.requestFocusInWindow();
                    }
            }

            @Override
            public void keyReleased(KeyEvent e)
            {
            }
        });
        buttonSend.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if(!inputArea.getText().isEmpty())
                {
                    output.println(inputArea.getText().replaceAll("\\s"," ").trim());
                    inputArea.setText("");
                    inputArea.requestFocusInWindow();
                }
            }
        });
        buttonExit.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    inputListener.interrupt();
                    allMessageAreaRefreshener.interrupt();
                    output.println("endOfSession");
                    if(socket!=null)
                        socket.close();
                    input.close();
                    output.close();
                }
                catch (Exception ex)
                {
                    System.exit(1);
                }
                System.exit(0);
            }
        });
    }


    public Client()
    {
        frame = new JFrame("Simple Client");
        frame.setSize(400,500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        welcomePage();

        frame.setVisible(true);

    }

    public void welcomePage()
    {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Your login:");
        panel.add(label);

        JTextField textField = new JTextField(15)
        {
            public void addNotify() {
                super.addNotify();
                requestFocus();
            }
        };


        panel.add(textField);

        JButton loginButton = new JButton("log in");
        panel.add(loginButton);

        JButton exitButton = new JButton("exit");
        panel.add(exitButton);
        frame.add(panel, BorderLayout.CENTER);
        buffer = new StringBuffer(4000);




        ActionListener listener = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if(textField.getText().isEmpty())
                {
                    JOptionPane.showMessageDialog(frame.getContentPane(), "Please enter your login");
                    textField.requestFocusInWindow();
                }
                else
                {
                    try
                    {
                        socket = new Socket(HOST, PORT);

                        output = new PrintWriter(socket.getOutputStream(),true);
                        input = new Scanner(socket.getInputStream());
                        output.println(textField.getText());
                    }
                    catch (Exception ex)
                    {
                        JOptionPane.showMessageDialog(frame.getContentPane(),"Can't connect Server");

                        System.exit(1);
                    }
                    login = textField.getText();
                    frame.remove(loginButton);
                    frame.remove(exitButton);
                    frame.remove(label);
                    frame.remove(textField);
                    frame.remove(panel);
                    addComponentsToPane(frame.getContentPane());
                    frame.pack();
                }
            }
        };

        loginButton.addActionListener(listener);
        textField.addKeyListener(new KeyListener()
        {
            @Override
            public void keyTyped(KeyEvent e)
            {

            }

            @Override
            public void keyPressed(KeyEvent event)
            {
                if (event.getKeyCode() == KeyEvent.VK_ENTER)
                {

                    if(textField.getText().isEmpty())
                    {
                        JOptionPane.showMessageDialog(frame.getContentPane(), "Please enter your login");
                        textField.requestFocusInWindow();
                    }
                    else
                    {
                        try
                        {
                            socket = new Socket(HOST, PORT);

                            output = new PrintWriter(socket.getOutputStream(),true);
                            input = new Scanner(socket.getInputStream());
                            output.println(textField.getText());
                        }
                        catch (Exception ex)
                        {
                            JOptionPane.showMessageDialog(frame.getContentPane(),"Can't connect Server");

                            System.exit(1);
                        }
                        login = textField.getText();
                        frame.remove(loginButton);
                        frame.remove(exitButton);
                        frame.remove(label);
                        frame.remove(textField);
                        frame.remove(panel);
                        addComponentsToPane(frame.getContentPane());
                        frame.pack();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e)
            {

            }
        });

        exitButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                System.exit(0);
            }
        });

    }

    public static void main(String[] args)
    {
        Client frame = new Client();
    }
}