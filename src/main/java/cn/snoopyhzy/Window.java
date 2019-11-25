package cn.snoopyhzy;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

/**
 * @program: druid sql ast gui
 * @description: Druid SQL AST GUI (java swing) [charset:UTF-8]
 * @author: snoopyhzy
 * @create: 2019-11-24 04:40
 */
public class Window {
    private static final boolean IS_ENGLISH = false;
    private static final String JFRAME_TITLE = IS_ENGLISH ? "Druid SQL AST GUI base on Druid %s" : "Druid SQL AST 生成展现小工具 基于Druid %s";
    private static final String CHOOSE_DATABASE = IS_ENGLISH ? "Choose Database:" : "数据库选择：";
    private static final String DO_SQL_PARSE = IS_ENGLISH ? "Parse SQL" : "执行SQL解析";
    private static final String PARSE_ERROR = IS_ENGLISH ? "Parse Failed" : "语法解析失败";
    private static final String DEMO_SQL = "select 'this is a mysql demo' from dual where 1<=>2";
    private static final String TREE_ROOT_STRING = IS_ENGLISH ? "Use %s Engine,Please double click Folder to view SQL AST"
            : "使用%s语法解析，解析后SQL语句列表请双击展开";
    private static final String LIST_ELEMENT_STRING = IS_ENGLISH ? "Element " : "元素";
    private static final String TEXT_IN_TREE_NODE = IS_ENGLISH ? "Name:%s,Class:%s,Content:%s" : "名称：%s，类型：%s，对象内容：%s";
    private static final String SQL_STATEMENT_STRING = IS_ENGLISH ? "SQL Staement " : "SQL语句";
    private static final String SEARCH_TREE_STR = IS_ENGLISH ? "Search AST Tree:" : "搜索AST抽象语法树：";
    private static final String SEARCH_STRING = IS_ENGLISH ? "Search" : "搜索";

    private Window(String sql, JTree jtree) {

        JFrame jFrame = new JFrame();
        jFrame.setTitle(String.format(JFRAME_TITLE, getDruidVersion()));
        jFrame.setBounds(10, 1, 1200, 800);//x,y坐标 和大小
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //中间可以用鼠标拖动的panel
        JSplitPane jSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        jSplitPane.setOneTouchExpandable(true);//让分割线显示出箭头
        jSplitPane.setContinuousLayout(true);//操作箭头，重绘图形
        jSplitPane.setDividerSize(2);
        jSplitPane.setDividerLocation(400);

        //语法树展示的面板在最中间
        JPanel centerPanel = new JPanel();
        BorderLayout centerPanelLayout = new BorderLayout();
        centerPanel.setLayout(centerPanelLayout);
        //搜索语法树的菜单
        JPanel searchPanel = new JPanel();
        JLabel searchLabel = new JLabel(SEARCH_TREE_STR);
        JButton searchButton = new JButton(SEARCH_STRING);
        JTextField searchField = new JTextField(20);
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        centerPanel.add(searchPanel, BorderLayout.NORTH);


        //树在中间
        JScrollPane jScrollPane1 = new JScrollPane();
        centerPanel.add(jScrollPane1, BorderLayout.CENTER);

        jScrollPane1.getViewport().add(jtree, null);
        jSplitPane.setRightComponent(centerPanel);

        //SEARCH EVENT
        searchButton.addActionListener(e -> {
            JTree tree = (JTree) jScrollPane1.getViewport().getComponent(0);
            visitAllNodeAndSelect(tree, (DefaultMutableTreeNode) jtree.getModel().getRoot(), searchField.getText());
        });

        //左边的总panel
        JPanel westPanel = new JPanel();
        BorderLayout borderLayout = new BorderLayout();
        //一个面板，里面放SQL语句
        JLabel databaseLabel = new JLabel(CHOOSE_DATABASE);
        JPanel databasePanel = new JPanel();
        databasePanel.add(databaseLabel);
        JComboBox<String> databaseBox = new JComboBox<>();
        databaseBox.addItem(JdbcConstants.ORACLE);
        databaseBox.addItem(JdbcConstants.MYSQL);
        databaseBox.addItem(JdbcConstants.HIVE);
        databaseBox.addItem(JdbcConstants.SQL_SERVER);
        databaseBox.addItem(JdbcConstants.POSTGRESQL);
        databaseBox.addItem(JdbcConstants.DB2);
        databaseBox.addItem(JdbcConstants.MARIADB);
        databaseBox.addItem(JdbcConstants.H2);
        databaseBox.addItem(JdbcConstants.ODPS);
        databaseBox.addItem(JdbcConstants.PHOENIX);
        databaseBox.addItem(JdbcConstants.ELASTIC_SEARCH);
        databaseBox.addItem("Other");
        databasePanel.add(databaseBox);

        westPanel.setLayout(borderLayout);
        westPanel.add(databasePanel, BorderLayout.NORTH);
        //文本框在滚动区域
        JTextArea textArea = new JTextArea(null, sql, 5, 40);
        textArea.setLineWrap(true);
        JScrollPane jScrollPane = new JScrollPane(textArea);
        jScrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        westPanel.add(jScrollPane, BorderLayout.CENTER);

        //执行SQL按钮
        JButton doParserButton = new JButton(DO_SQL_PARSE);
        westPanel.add(doParserButton, BorderLayout.SOUTH);

        doParserButton.addActionListener(e -> {
            try {
                jtree.setModel(new DefaultTreeModel(parseSQLtoTree(textArea.getText(), (String) databaseBox.getSelectedItem()), false));
            } catch (Exception ex) {
                DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(PARSE_ERROR);
                dmtn.add(new DefaultMutableTreeNode(ex.toString()));
                jtree.setModel(new DefaultTreeModel(dmtn, false));

            }
            jtree.repaint();
        });
        jSplitPane.setLeftComponent(westPanel);
        jFrame.add(jSplitPane);

        jFrame.setVisible(true);
    }

    private void visitAllNodeAndSelect(JTree tree, DefaultMutableTreeNode node, String text) {
        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            TreePath path = new TreePath(child.getPath());
            String str = child.getUserObject().toString();
            if (!"".equals(text.trim()) && str.contains(text)) {
                tree.addSelectionPath(path);
                if (!child.isLeaf()) {
                    tree.expandPath(path);
                }
            } else {
                tree.removeSelectionPath(path);
            }
            visitAllNodeAndSelect(tree, child, text);
        }
    }

    public static void main(String[] args) {
        String dbType = JdbcConstants.MYSQL;
        JTree jTree = new JTree(parseSQLtoTree(DEMO_SQL, dbType));
        new Window(DEMO_SQL, jTree);
    }

    private static DefaultMutableTreeNode parseSQLtoTree(String sql, String dbType) {
        List<SQLStatement> statementList = SQLUtils.parseStatements(sql, dbType);
        DefaultMutableTreeNode defaultMutableTreeNode = new DefaultMutableTreeNode(String.format(TREE_ROOT_STRING, dbType));
        int i = 0;
        for (SQLStatement statement : statementList) {
            defaultMutableTreeNode.add(buildMutableTree(new TreeObject(SQL_STATEMENT_STRING + (++i), statement)));
        }
        return defaultMutableTreeNode;
    }


    private static DefaultMutableTreeNode buildMutableTree(TreeObject treeObject) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(treeObject);
        Object object = treeObject.getObject();
        Class<?> objClass = object.getClass();
        if (object instanceof List) {
            //druid很多类型封在list里需要对list做特殊解析
            int i = 0;
            for (Object objInList : (List) object) {
                treeNode.add(buildMutableTree(new TreeObject(LIST_ELEMENT_STRING + (i++), objInList)));
            }
        }
        if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            Set<Map.Entry<?, ?>> entrySet = ((Map) object).entrySet();
            for (Map.Entry entry : entrySet) {
                treeNode.add(buildMutableTree(new TreeObject(entry.getKey().toString(), entry.getValue())));
            }
        }
        if (!objClass.getTypeName().startsWith("com.alibaba.druid")) {
            return treeNode;
        }
        Map<String, Method> methodMap = new LinkedHashMap<>();
        for (Method method : objClass.getMethods()) {
            String methodName = method.getName();
            if (method.getParameterCount() > 0 || "getClass".equals(methodName)) {
                continue;
            }
            String fieldName = null;
            if (methodName.startsWith("get")) {
                char[] chars = methodName.substring(3).toCharArray();
                chars[0] += 32;//大写转小写
                fieldName = String.valueOf(chars);
            } else if (methodName.startsWith("is")) {
                char[] chars = methodName.substring(2).toCharArray();
                chars[0] += 32;//大写转小写
                fieldName = String.valueOf(chars);
            } else {
                continue;
            }
            //support show getParent but only itself.
            if ("getParent".equals(methodName)) {
                try {
                    treeNode.add(new DefaultMutableTreeNode(new TreeObject(fieldName, method.invoke(object))));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                continue;
            }

            methodMap.put(fieldName, method);
        }
        //排序处理
        methodMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(
                entry -> {
                    try {
                        Object obj;
                        if ((obj = entry.getValue().invoke(object)) != null) {
                            treeNode.add(buildMutableTree(new TreeObject(entry.getKey(), obj)));
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
        );

        return treeNode;
    }

    private static String getDruidVersion() {
        String version = "";
        Properties prop = new Properties();
        try (InputStream input = Window.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            prop.load(input);
            return prop.getProperty("Druid-Version", "");
            //https://stackoverflow.com/questions/58236241/openjdk-11-0-4-has-java-atk-wrapper-jar-in-the-classpath
        } catch (IOException e) {
            e.printStackTrace();
        }
        return version;
    }

    private static class TreeObject {
        private String name;
        private Object object;

        public TreeObject(String name, Object object) {
            this.name = name;
            this.object = object;
        }

        public Object getObject() {
            return object;
        }

        @Override
        public String toString() {
            return String.format(TEXT_IN_TREE_NODE, name, object == null ? "null" : object.getClass(),
                    object == null ? "null" : object.toString());
        }
    }

}
