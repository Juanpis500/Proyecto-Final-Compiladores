
import com.formdev.flatlaf.FlatIntelliJLaf;
import compilerTools.CodeBlock;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import compilerTools.Directory;
import compilerTools.ErrorLSSL;
import compilerTools.Functions;
import compilerTools.Grammar;
import compilerTools.Production;
import compilerTools.TextColor;
import compilerTools.Token;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.Timer;

/**
 *
 * @author yisus
 */
public class Compilador extends javax.swing.JFrame {

    private String title; //Es eel titulo del compilador
    private Directory Directorio; //Nos permit abrir los archivos
    private ArrayList<Token> tokens; //Almacena los tokens qu tenemos
    private ArrayList<ErrorLSSL> errores; //Almacna los erores qu vayamos encontrando
    private ArrayList<TextColor> textsColor; //Vamos a guardar los colores
    private Timer timerKeyReleased; // nos prmit colorear las palabras reservadas
    private ArrayList<Production> producciones; // Para almacenar als producciones geeneradas del analisador sintactico
    private HashMap<String, String> identificadores; //Los  ideentificadors qu gaurdaremos
    private boolean compiladorCompilado = false;

    /**
     * Creates new form Compilador
     */
    public Compilador() {
        initComponents();
        init();
    }

    private void init() {
        title = "IDE LARA";
        setLocationRelativeTo(null);
        setTitle(title);
        Directorio = new Directory(this, jtpCode, title, ".la");
        addWindowListener(new WindowAdapter() {// Cuando presiona la "X" de la esquina superior derecha
            @Override
            public void windowClosing(WindowEvent e) {
                Directorio.Exit();
                System.exit(0);
            }
        });
        //Nos permite agregar numeracion a el campo de texto
        Functions.setLineNumberOnJTextComponent(jtpCode);
        timerKeyReleased = new Timer (300, ((e) -> {
            timerKeyReleased.stop();
            colorAnalysis();
        }));
        //Altera el nombre del  idee para agregar un asteristco  cada que modifiqueemos
        //el area donde va ele codigo
        Functions.insertAsteriskInName(this, jtpCode, () ->{
            timerKeyReleased.restart();
        });
        
        tokens = new ArrayList<>();
        errores = new ArrayList<>();
        textsColor = new ArrayList<>();
        producciones = new ArrayList<>();
        identificadores = new HashMap<>();
        
        //Ahora agregamos un metodo que nos permite agreagr un autocompletador
        Functions.setAutocompleterJTextComponent(new String[]{"main","print","function"}, jtpCode, () ->{
            timerKeyReleased.restart();
            
        });
    }

    private void colorAnalysis(){
        textsColor.clear();
        LexerColor lexer;
        
        try {
            File codigo = new File("color.encrypter");
            FileOutputStream output = new FileOutputStream(codigo);
            byte[] byteText = jtpCode.getText().getBytes();
            output.write(byteText);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(new FileInputStream(codigo), "UTF-8"));
            lexer = new LexerColor(entrada);
            while(true){
                TextColor textColor = lexer.yylex();
                if(textColor == null ){
                    break;
                }
                textsColor.add(textColor);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Compilador.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Compilador.class.getName()).log(Level.SEVERE, null, ex);
        }
        Functions.colorTextPane(textsColor, jtpCode, Color.BLACK);
    }
    
    private void limpiar() {
        Functions.clearDataInTable(tblTokens);
        jtaOutputConsole.setText("");
        tokens.clear();
        errores.clear();
        producciones.clear();
        identificadores.clear();
        compiladorCompilado = false;
    }
    
    private void compilar(){
        limpiar();
        anlisisLexico();
        llenarTokens();
        analisisSintactico();
        analisisSemantico();
        imprimirConsola();
        compiladorCompilado = true;
    }
    
    private void anlisisLexico(){
        Lexer lexer;
        
        try {
            File codigo = new File("code.encrypter");
            FileOutputStream output = new FileOutputStream(codigo);
            byte[] byteText = jtpCode.getText().getBytes();
            output.write(byteText);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(new FileInputStream(codigo), "UTF-8"));
            lexer = new Lexer(entrada);
            while(true){
                Token token = lexer.yylex();
                if(token == null ){
                    break;
                }
                tokens.add(token);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Compilador.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Compilador.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void llenarTokens(){
        tokens.forEach(token ->{
            Object[] data = new Object[]{token.getLexicalComp(), token.getLexeme(), 
                "[" + token.getLine() + ", " + token.getColumn() + "]"};
            Functions.addRowDataInTable(tblTokens, data);
        });
    }
    
    private void analisisSintactico(){
        Grammar gramatica = new Grammar(tokens, errores);
        
        /* Agrupaciones  de declaraciones*/
        
        gramatica.group("LISTA_ID", "IDENTIFICADOR (COMA IDENTIFICADOR)+");
        gramatica.group("LISTA_ID", "IDENTIFICADOR (COMA)+", 1,
                "ERROR sintactico {}: Falta el identificacdor al lado de la coma en linea #");
        gramatica.group ("DECLARACION","TIPO_DATO (IDENTIFICADOR | LISTA_ID)", true, producciones);
        gramatica.group ("DECLARACION","TIPO_DATO ", true, 2,
                "ERROR sintactico {}: Falta el identificador en la declaracion linea #");
        
        gramatica.group("VALOR", "(FLOTANTE | ENTERO)", true);
        
         /* Agruapaciones de operaciones*/
        gramatica.group("OP_ARITMETICA", " (VALOR | IDENTIFICADOR) (OP_ARIT (VALOR | IDENTIFICADOR))+");
        gramatica.group("OP_ARITMETICA", " (VALOR | IDENTIFICADOR) (OP_ARIT)+", 20, 
                "ERROR sintactico: Falta el valor en la operacion de la linea #");
        gramatica.group("OP_ARITMETICA", " (VALOR | IDENTIFICADOR) ((VALOR | IDENTIFICADOR))+", 21, 
                "ERROR sintactico: Falta un operador en la linea #");
        
        /*Agrupaciones de asignaciones*/
        gramatica.group("ASIGNACION", "IDENTIFICADOR OP_ASIG (VALOR | OP_ARITMETICA | BOOLEANO | IDENTIFICADOR)", true, producciones);
        gramatica.group("ASIGNACION", "IDENTIFICADOR (VALOR | OP_ARITMETICA | BOOLEANO | IDENTIFICADOR)", true, 4, 
                "ERROR sintactico: Falta el operador de asignacion en la asignacion linea #");
        gramatica.group("ASIGNACION", "IDENTIFICADOR OP_ASIG", true, 5,
                "ERROR sintactico: Falta el valor en la asignacion linea #");
        gramatica.group("ASIGNACION", " OP_ASIG (VALOR | OP_ARITMETICA | BOOLEANO | IDENTIFICADOR)", true, 5,
                "ERROR sintactico: Falta el identificador en la asignacion linea #");
        
        /*Agrupaciones de  lecruta y escritura*/
        gramatica.group("ESCRITURA"," WRITE (ASIGNACION | IDENTIFICADOR | VALOR)",true);
        gramatica.group("ESCRITURA"," WRITE ",true, 25,
                "ERROR sintactico {}: Falta el parametro para la impresion en la linea #");
       
        gramatica.group("LECTURA", " READ (ASIGNACION | IDENTIFICADOR | VALOR)",true);
        gramatica.group("LECTURA"," READ ",true, 25,
                "ERROR sintactico {}: Falta el parametro para la lectura en la linea #");
        
        /* Eliminacion de tipos de datos y operadores de asignacion solitarios*/
        gramatica.delete("TIPO_DATO", 6, "ERROR sintactico: EL tipo de dato no esta en una declaracion linea #");
        gramatica.delete("OP_ASIG", 7, "ERROR sintactico: El operador de asignacion no esta en una declaracion o asignacion linea #");
        gramatica.delete("COMA", 10, "ERROR sintactico: La coma no esta asignada a ninguna declaracion o sentencia linea #");
        gramatica.delete("OP_ARIT", 10, "ERROR sintactico: El operador aritmetico no esta asignao a ninguna operacion o sentencia linea #");
        
        /* Agrupar los identificadores y definicion de parametros*/
        gramatica.group("VALOR", "IDENTIFICADOR", true);
        gramatica.group("PARAMETROS_COMP", "VALOR (IGUAL | MENOR | MAYOR | MAYOR_IGUAL | MENOR_IGUAL | DIFERENTE) VALOR",true);
        gramatica.group("PARAMETROS_COMP", "VALOR (IGUAL | MENOR | MAYOR | MAYOR_IGUAL | MENOR_IGUAL | DIFERENTE) ",true, 8, 
                "ERROR sintactico: Falta el valor del lado derecho de la compraracion linea # columna %");
        gramatica.group("PARAMETROS_COMP", " (IGUAL | MENOR | MAYOR | MAYOR_IGUAL | MENOR_IGUAL | DIFERENTE) VALOR",true, 9, 
                "ERROR sintactico: Falta el valor del lado izquierdo de la compraracion linea # columna %");
        gramatica.group("PARAMETROS_COMP", " VALOR  VALOR",true, 9, 
                "ERROR sintactico: Falta el operador de comapracion en la linea # columna %");
        
        /*Agrupacion de eexpresion logica*/
        gramatica.loopForFunExecUntilChangeNotDetected(()->{
            gramatica.group("EXP_LOGICA", "(PARAMETROS_COMP | EXP_LOGICA)(OP_LOGICO (PARAMETROS_COMP | EXP_LOGICA))+");
            gramatica.group("EXP_LOGICA", "PARENTESIS_A (PARAMETROS_COMP | EXP_LOGICA) PARENTESIS_C");
        });
        
        /* Eliminacion de operadores logicos*/
        gramatica.delete("OP_LOGICO", 10, "ERROR sintactico: el operador logico no esta conenido en una expresion linea #");
        
        /* Verificacion de puntos y comas */
        gramatica.finalLineColumn();
        /* Declaraciones, asignaciones Y LECTURA ESCRITURA*/
        gramatica.group("DECLARACION_PC", "DECLARACION PUNTO_COMA", true);
        gramatica.group("DECLARACION_PC", "DECLARACION", true, 18,
                "ERROR sintactico: Falta el punto y coma ; en la linea #");
     
        gramatica.group("ASIGNACION_PC", "ASIGNACION PUNTO_COMA", true);
        gramatica.group("ASIGNACION_PC", "ASIGNACION", true, 19, 
                "ERROR sintactico: Falta el punto y coma ; en la linea #");
        
        gramatica.group("LECTURA_PC", "LECTURA PUNTO_COMA", true);
        gramatica.group("LECTURA_PC", "LECTURA", true, 19, 
                "ERROR sintactico: Falta el punto y coma ; en la linea #");
        
        gramatica.group("ESCRITURA_PC", "ESCRITURA PUNTO_COMA", true);
        gramatica.group("ESCRITURA_PC", "ESCRITURA", true, 19, 
                "ERROR sintactico: Falta el punto y coma ; en la linea #");
        
        gramatica.initialLineColumn();
        
        /* Eliminacion de punto y coma*/
        gramatica.delete("PUNTO_COMA", 22, 
                "ERROR sintactico: El punto y coma ; no esta al final de una sentencia linea #");
        
        /* Agrupaciones de sentencias */
        gramatica.group("SENTENCIAS", "(DECLARACION_PC | ASIGNACION_PC | LECTURA_PC | ESCRITURA_PC)+");
        
        gramatica.group("ESTRUCT_DO_UNTIL", "CICLO LLAVE_A  LLAVE_C CICLO EXP_LOGICA ", true);
        /* Estructura if y ciclos*/
        gramatica.loopForFunExecUntilChangeNotDetected(()->{
            gramatica.group("ESTRUCT_IF","IF EXP_LOGICA THEN LLAVE_A  LLAVE_C ELSE LLAVE_A LLAVE_C FI", true);
            gramatica.group("ESTRUCT_IF","IF EXP_LOGICA THEN LLAVE_A SENTENCIAS LLAVE_C ELSE LLAVE_A SENTENCIAS LLAVE_C FI", true);
            gramatica.group("ESTRUT_WHILE","CICLO PARENTESIS_A (EXP_LOGICA | VALOR)? PARENTESIS_C LLAVE_A (SENTENCIAS)? LLAVE_C", true);
            gramatica.group("ESTRUCT_DO_UNTIL", "CICLO LLAVE_A (SENTENCIAS)? LLAVE_C CICLO EXP_LOGICA", true);
            
            
            gramatica.group("SENTENCIAS", "(SENTENCIAS | ESTRUCT_IF| ESTRUT_WHILE | ESTRUCT_DO_UNTIL )+");
        });
        
        
        gramatica.group("ESTR_PROGRAM", "PROGRAM LLAVE_A (SENTENCIAS)? LLAVE_C",true);
        gramatica.group("ESTR_PROGRAM", "PROGRAM LLAVE_A (SENTENCIAS)? ",true, 30, 
                "ERROR sintactico: Falta la llave que cierra linea #");
        gramatica.group("ESTR_PROGRAM", "PROGRAM (SENTENCIAS)? LLAVE_C",true,31,
                "ERROR sintactico: Falta la llave que abre linea #");
        
        /*Eliminacion de program sin llaves*/
        gramatica.delete("PROGRAM",32, "ERROR sintactico {}: Esta palabra se encuentra sin asignacion linea # ");
        gramatica.show();
    }
    
    private void analisisSemantico(){
        HashMap<String, String> identDataType = new HashMap<>();
        identDataType.put("int", "ENTERO");
        identDataType.put("bool", "BOOLEANO");
        identDataType.put("float","FLOTANTE");
        for (Production id: producciones){
            if("TIPO_DATO".equals(id.lexicalCompRank(0)) ){
                for(int i = 1; i<= id.getSizeTokens(); i++){
                    if("IDENTIFICADOR".equals(id.lexicalCompRank(i))){
                        if(!identDataType.containsKey(id.lexemeRank(i))){
                            identDataType.put(id.lexemeRank(i), id.lexemeRank(0));
                        }else{
                            errores.add(new ErrorLSSL(1, "ERROR semantico {}: La variable ya ha sido declarada linea #", id, true));
                        }
                    }
                }
            }
            if("IDENTIFICADOR".equals(id.lexicalCompRank(0))){
                
                if(id.getSizeTokens() == 3){
                    
                    if(!identDataType.containsKey(id.lexemeRank(0))){
                        errores.add(new ErrorLSSL(1, "ERROR semantico {}: La variable no se ha declarado en la linea # columna %", id, true));
                    }else{
                        
                        if("IDENTIFICADOR".equals(id.lexicalCompRank(2))){
                            if(!identDataType.containsKey(id.lexemeRank(2))){
                                errores.add(new ErrorLSSL(1, "ERROR semantico {}: La variable no se ha declarado en la linea # columna %", id, true));
                            }else{
                                if(!identDataType.get(id.lexemeRank(0)).equals(identDataType.get(id.lexemeRank(2)))){
                                    errores.add(new ErrorLSSL(1, "ERROR semantico {}: No se puede asignar un tipo "+ identDataType.get(id.lexemeRank(0)) + " con un tipo "+ identDataType.get(id.lexemeRank(2)) +" linea #", id, true));
                                    System.out.println("Si entro");
                                }
                            }
                        }else{
                            
                        }
                    }
                }
            }
        }
        
        for (String key: identDataType.keySet()){  
            Object[] data = new Object[]{key, identDataType.get(key)};
            Functions.addRowDataInTable(tblSeman, data);
            System.out.println(key+ " = " + identDataType.get(key));
	}
    }
    
    private void imprimirConsola(){
        int numErrores = errores.size();
        if(numErrores > 0){
            Functions.sortErrorsByLineAndColumn(errores); //Ordnamos los errores por numero de linea y coluumna
            String strErrores = "\n";
            for(ErrorLSSL error : errores){
                String strError = String.valueOf(error);
                strErrores += strError + "\n";
            }
             jtaOutputConsole.setText("Compilacion terminada... \n" + strErrores + 
                     "\n La Compilacion termino con errores");
        }else{
            jtaOutputConsole.setText("Compilacion terminada... \n");
        }
    }
    
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jSplitPane2 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtpCode = new javax.swing.JTextPane();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblTokens = new javax.swing.JTable();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        jScrollPane5 = new javax.swing.JScrollPane();
        tblSeman = new javax.swing.JTable();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtaOutputConsole = new javax.swing.JTextPane();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        btnNuevo = new javax.swing.JMenuItem();
        btnAbrir = new javax.swing.JMenuItem();
        btnGuardar = new javax.swing.JMenuItem();
        btnGuardarC = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        btnCompilar = new javax.swing.JMenuItem();
        btnEjecutar = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setBackground(new java.awt.Color(102, 102, 102));
        setMinimumSize(new java.awt.Dimension(1280, 720));
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.LINE_AXIS));

        jSplitPane1.setDividerLocation(450);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jSplitPane2.setDividerLocation(700);
        jSplitPane2.setMinimumSize(new java.awt.Dimension(193, 350));

        jtpCode.setBackground(new java.awt.Color(255, 255, 255));
        jtpCode.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jtpCode.setForeground(new java.awt.Color(255, 255, 255));
        jtpCode.setMinimumSize(new java.awt.Dimension(700, 22));
        jScrollPane1.setViewportView(jtpCode);

        jSplitPane2.setLeftComponent(jScrollPane1);

        tblTokens.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Componente Lexico", "Lexema", "Linea - Columna"
            }
        ));
        tblTokens.setAutoscrolls(false);
        jScrollPane2.setViewportView(tblTokens);

        jTabbedPane1.addTab("Lexico", jScrollPane2);

        jScrollPane4.setViewportView(jTree1);

        jTabbedPane1.addTab("Sintactico", jScrollPane4);

        tblSeman.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Key", "Dato"
            }
        ));
        jScrollPane5.setViewportView(tblSeman);

        jTabbedPane1.addTab("Semantico", jScrollPane5);

        jSplitPane2.setRightComponent(jTabbedPane1);
        jTabbedPane1.getAccessibleContext().setAccessibleName("Lexico");

        jSplitPane1.setLeftComponent(jSplitPane2);

        jTabbedPane2.setForeground(new java.awt.Color(43, 53, 68));

        jtaOutputConsole.setBackground(new java.awt.Color(51, 51, 51));
        jtaOutputConsole.setFont(new java.awt.Font("Lucida Console", 0, 14)); // NOI18N
        jtaOutputConsole.setForeground(new java.awt.Color(51, 204, 0));
        jScrollPane3.setViewportView(jtaOutputConsole);

        jTabbedPane2.addTab("Consola", jScrollPane3);

        jSplitPane1.setRightComponent(jTabbedPane2);

        getContentPane().add(jSplitPane1);

        jMenu1.setText("Archivo");

        btnNuevo.setText("Nuevo");
        btnNuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoActionPerformed(evt);
            }
        });
        jMenu1.add(btnNuevo);

        btnAbrir.setText("Abrir");
        btnAbrir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAbrirActionPerformed(evt);
            }
        });
        jMenu1.add(btnAbrir);

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });
        jMenu1.add(btnGuardar);

        btnGuardarC.setText("Guardar Como");
        btnGuardarC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarCActionPerformed(evt);
            }
        });
        jMenu1.add(btnGuardarC);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        jMenu3.setText("Compilador");

        btnCompilar.setText("Compilar");
        btnCompilar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompilarActionPerformed(evt);
            }
        });
        jMenu3.add(btnCompilar);

        btnEjecutar.setText("Ejecutar");
        btnEjecutar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEjecutarActionPerformed(evt);
            }
        });
        jMenu3.add(btnEjecutar);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        Directorio.New();
        limpiar();
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnAbrirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAbrirActionPerformed
        if(Directorio.Open()){
            colorAnalysis();
            limpiar();
        }
    }//GEN-LAST:event_btnAbrirActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        if(Directorio.Save()){
            limpiar();
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnGuardarCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarCActionPerformed
        if(Directorio.SaveAs()){
            limpiar();
        }
    }//GEN-LAST:event_btnGuardarCActionPerformed

    private void btnCompilarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCompilarActionPerformed

        if(getTitle().contains("*") || getTitle().equals(title)){
            if(Directorio.Save()){
                compilar();
            }
        }else{
            compilar();
        }
    }//GEN-LAST:event_btnCompilarActionPerformed

    private void btnEjecutarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEjecutarActionPerformed
        btnCompilar.doClick();
        if(compiladorCompilado){
            if(errores.size() > 0){
                JOptionPane.showMessageDialog(null, "No se puede ejcutar el codigo poprqu hay erores");
            }else{
                
            }
        }
    }//GEN-LAST:event_btnEjecutarActionPerformed

    
    
    
    
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Compilador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Compilador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Compilador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Compilador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        java.awt.EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatIntelliJLaf());
            } catch (UnsupportedLookAndFeelException ex) {
                System.out.println("LookAndFeel no soportado: " + ex);
            }
            new Compilador().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem btnAbrir;
    private javax.swing.JMenuItem btnCompilar;
    private javax.swing.JMenuItem btnEjecutar;
    private javax.swing.JMenuItem btnGuardar;
    private javax.swing.JMenuItem btnGuardarC;
    private javax.swing.JMenuItem btnNuevo;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTree jTree1;
    private javax.swing.JTextPane jtaOutputConsole;
    private javax.swing.JTextPane jtpCode;
    private javax.swing.JTable tblSeman;
    private javax.swing.JTable tblTokens;
    // End of variables declaration//GEN-END:variables

    
}
