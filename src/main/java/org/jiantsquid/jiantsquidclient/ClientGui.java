package org.jiantsquid.jiantsquidclient;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.jiantsquid.core.application.JiantsquidClientApplicationI;
import org.jiantsquid.core.database.DatabaseClient;
import org.jiantsquid.core.identity.Account;
import org.jiantsquid.core.identity.Identity;
import org.jiantsquid.core.sandbox.Sandbox;
import org.jiantsquid.core.smartcontractgui.SmartContractAPI;
import org.jiantsquid.core.smartcontractgui.SmartContractGUI;
import org.jiantsquid.core.utils.SillyUtils;
import org.jiantsquid.jiantsquidapp.database.SmartCode;
import org.jiantsquid.jiantsquidapp.test.RunDatabaseCluster;
import org.jiantsquid.network.p2p.message.Request;
import org.jiantsquid.network.p2p.message.Response;
import org.jiantsquid.network.p2p.service.Actions;

public class ClientGui implements JiantsquidClientApplicationI {
	
	private ClientApi clientApi ;
	private DatabaseClient databaseClient ;
	
	private JLabel label ;
	private JFrame frame ;
	private Component currentComponent ;
	
	private Identity entity ;
	private String accountFileName ;
	
	void start() throws ClassNotFoundException, IOException {
		init() ;
		
		entity = new Identity() {

			private static final long serialVersionUID = 1L;

			@Override
            public boolean is(Identity entity) {
                return false;
            }

            @Override
            public String getId() {
                return null;
            }
            
        } ;
        
		SwingUtilities.invokeLater( () -> {
			try {
				install();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} ) ;
	}
	
	private void init() throws ClassNotFoundException, IOException {

	}
	
	void install() throws Exception {
		frame = new JFrame() ;
		frame.setLayout( new BorderLayout() ) ;
		
		Map<String,String> parameters = new HashMap<>() ;
		parameters.put( Actions.WHAT, Actions.LOAD_MAINGUI_PARAM ) ;
		
		label = new JLabel( "Connecting..." ) ;
		frame.add( label ) ;
		
		frame.setMinimumSize( new Dimension( 800, 600 ) ) ;
		frame.pack();
		frame.setVisible( true ) ;
		frame.validate() ;
		
		if( true ) {
			SwingUtilities.invokeLater( new Runnable() {

				@Override
				public void run() {
					try {
						//clientApi = new ClientApi( "localhost", 33000 + 4  ) ;
						databaseClient  = new DatabaseClient( "localhost", 
								           RunDatabaseCluster.DATABASE_CLUSTER_PORT + 19) ;
//						NetworkIdentity me = new NetworkIdentity( getHost(), getPort() ) ;
//						Response response = clientApi.send( new Request( me, Actions.LOAD_SERVICE_ACTION, parameters ) ) ;
						
						Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() ) ;
						SmartCode code = (SmartCode) databaseClient.getClient().getMap( "SMARTCODE" )
								.get( "SEARCH_ENGINE" ) ;
						String serviceCode = code.getCode() ; //response.getParameter( "MAIN_CODE" ) ;
						Sandbox sandbox = new Sandbox( code.getMainclass(), serviceCode );
						
						frame.remove( label ) ;
						frame.add( (JComponent) sandbox.getAppClass().getDeclaredConstructor().newInstance() ) ;
						frame.validate() ;
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					
				}
				
			});
			
		}
		
		
		System.out.println( "Client started" ) ;
	}
	public static void main( String[] args ) throws ClassNotFoundException, IOException {
		new ClientGui().start() ;
	}
	
	private void updateLabel( String text ) {
	    label.setText( text ) ;
        label.repaint();
        frame.validate() ;
	}
	
	public void openService( String service ) throws NoSuchAlgorithmException, NoSuchProviderException {
	    
	    updateLabel( "Connecting..." ) ;
	    
	    Map<String,String> parameters = new HashMap<>() ;
        parameters.put( Actions.WHAT, service ) ;
        
	    Response response = null ;
        try {
            response = clientApi.send( new Request( entity, Actions.LOAD_SERVICE_ACTION, parameters ) );
        } catch (IOException e) {
            updateLabel( "Connection failed" ) ;
        }
        
        
        updateLabel( "loading..." ) ;
        if( response == null ) {
            SillyUtils.todo( "do something when response null ", null );
            return ;
        }
        String serviceCode = response.getParameter( "MAIN_CODE" ) ;
        String mainClass = response.getParameter( "MAIN_CLASS" ) ;
	    Sandbox sandbox = new Sandbox( mainClass, serviceCode );
        
        SmartContractGUI<JComponent> contract = null ;
        try {
            contract = sandbox.getContract();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            SillyUtils.todo( "manage client exception to getContract ", e );
        }
        SmartContractAPI api = null ;
        try {
            api = new SmartContractAPI( this, getAccount() );
        } catch (ClassNotFoundException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if( api != null ) {
            contract.activate( api );
            
            updateLabel( "updating..." ) ;
            frame.remove(currentComponent);
            currentComponent = contract.getComponent() ;
            frame.add( currentComponent ) ;
        } else {
            updateLabel( "Could not load account" ) ;
        }
        frame.validate() ;
	}
	
	private Account getAccount() throws FileNotFoundException, IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchProviderException {
	    Account account = null ;
	    boolean accountExists = true ;
	    String homeDirectory = System.getProperty( "user.home" ) ;
	    String jsqDirectory = homeDirectory + File.separator + ".jiantsquid" ;
	    accountFileName = jsqDirectory + File.separator + "account" ;
	    File accountDir = new File( jsqDirectory ) ;
	    if( !accountDir.exists() ) {
	        accountDir.mkdir() ;
	        accountExists = false ;
	    }
	    
	    File accountFile = new File( accountFileName ) ;
	    if( !accountFile.exists() ) {
            accountExists = false ;
        }
	    if( !accountExists ) {
	        account = new Account() ;
	        saveAccount( account ) ;
	    } else {
	        account = readAccount() ;
	    }
	    
	    return account ;
	}
	
	private org.jiantsquid.core.identity.Account readAccount() throws IOException, ClassNotFoundException {
	    ObjectInputStream stream = new ObjectInputStream( new FileInputStream( accountFileName ) ) ;
	    Account account = (Account) stream.readObject() ;
	    stream.close();
	    return account ;
	}
	
	private void saveAccount( Account account ) throws FileNotFoundException, IOException {
	    ObjectOutputStream stream = new ObjectOutputStream( new FileOutputStream( accountFileName  )) ;
	    stream.writeObject( account );
	    stream.close();
    }
}
