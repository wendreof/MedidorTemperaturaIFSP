package wendreo.fernandes.medidortemperaturaifsp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
	
	private static final int CODIGO_PARA_ATIVACAO_BLUETOOTH = 1;
	private static final UUID MEU_UUID = UUID.fromString ( "00001101-0000-1000-8000-00805F9B34FB" );
	String endereco_MAC_do_Bluetooth_remoto = "00:13:EF:00:1B:C0";
	Switch conectar;
	Button config;
	Button receberDadosTemperatura;
	TextView medicoesRecebidas;
	LinearLayout linearLayout;
	// representa um dispositivo bluetooth remoto
	private BluetoothDevice dispositivoBluetoothRemoto;
	//representa o adaptador Bluetooth do dispositivo local
	/*
	 * O BluetoothAdapter permite executar tarefas fundamentais do Bluetooth,
	 * como iniciar a descoberta de dispositivos,
	 * consultar uma lista de dispositivos ligados (emparelhados),
	 * instanciar um dispositivo Bluetooth usando um endereço MAC conhecido
	 * e criar um BluetoothServerSocket para ouvir solicitações de conexão
	 * de outros dispositivos e iniciar uma procura de dispositivos Bluetooth
	 * */
	private BluetoothAdapter meuBluetoothAdapter = null;
	//um soquete bluetooth conectado ou conectando
    /*A interface para soquetes Bluetooth é semelhante à dos soquetes TCP: Socket e ServerSocket.
    No lado do servidor, use um BluetoothServerSocket para criar um soquete
    de servidor de escuta.
    Quando uma conexão é aceita pelo BluetoothServerSocket,
    ele retorna um novo BluetoothSocket para gerenciar a conexão.
    No lado do cliente, use um único BluetoothSocket para iniciar
    uma conexão de saída e gerenciar a conexão.
    */
	private BluetoothSocket bluetoothSocket = null;
	/*
	 * Essa classe abstrata é a superclasse de todas as classes que representam
	 * um fluxo de entrada de bytes.
	 * Os aplicativos que precisam definir uma subclasse de InputStream
	 * sempre devem fornecer um método que retorne o próximo byte de entrada.*/
	private InputStream inputStream = null;
	
	/*
	 * A classe OutputStream abstrata é a superclasse de todas as
	 * classes que representam um fluxo de saída de bytes*/
	private OutputStream outStream = null;
	
	@Override
	protected void onCreate ( Bundle savedInstanceState ) {
		
		super.onCreate ( savedInstanceState );
		setContentView ( R.layout.activity_main );
		fazerConexoesDoLayout_e_Listeners ( );
		verificarCondicoesDoBlueTooth ( );
	}
	
	public void fazerConexoesDoLayout_e_Listeners ( ) {
		conectar = findViewById ( R.id.conectar );
		receberDadosTemperatura = findViewById ( R.id.btnMedirTemperatura );
		medicoesRecebidas = findViewById ( R.id.edtTxtResultadoMedicao );
		config = findViewById ( R.id.btnConfig );
		linearLayout = findViewById ( R.id.Linear );
		
		//eventos associados ao respectivos botões
		conectar.setOnClickListener ( this );
		config.setOnClickListener ( this );
		receberDadosTemperatura.setOnClickListener ( this );
		
	} // fim do método fazerConexoesDoLayout_e_Listeners
	
	public void verificarCondicoesDoBlueTooth ( ) {
		
		//identifique o adaptador Bluetooth local padrão
		meuBluetoothAdapter = BluetoothAdapter.getDefaultAdapter ( );
		
		// verifica se o celular tem bluetooth
		if ( meuBluetoothAdapter == null ) {
			
			ShowMSG ( "Dispositivo não possui adaptador Bluetooth" );
			// finaliza a aplicação
			finish ( );
		} else {
			// verifica se o bluetooth está desligado. Se sim, pede permissão para ligar
			if ( !meuBluetoothAdapter.isEnabled ( ) ) {
				Intent novoIntent = new Intent ( BluetoothAdapter.ACTION_REQUEST_ENABLE );
				startActivityForResult ( novoIntent, CODIGO_PARA_ATIVACAO_BLUETOOTH );
			}
		}// fim do else
	} // fim do método verificarCondicoesDoBlueTooth
	
	// método sobrescrito que irá ser chamado após o clique da mensagem
	@Override
	protected void onActivityResult ( int requestCode, int resultCode, Intent data ) {
		super.onActivityResult ( requestCode, resultCode, data );
		
		switch ( requestCode ) {
			case CODIGO_PARA_ATIVACAO_BLUETOOTH:
				if ( resultCode == Activity.RESULT_OK ) {
					
					ShowMSG ( "Bluetooth foi ativado" );
				} else {
					
					ShowMSG ( "Bluetooth não foi ativado" );
				}
				break;
		}
	} // fim do método onActivityResult
	
	@Override
	public void onClick ( View view ) {
		if ( view.getId ( ) == R.id.conectar ) {
			if ( conectar.isChecked ( ) ) {
				
				medicoesRecebidas.setText ( "" );
				
				//Valide um endereço Bluetooth, como "00: 43: A8: 23: 10: F0"
				// (os caracteres alfabéticos devem estar em maiúsculas para serem válidos)
				if ( BluetoothAdapter.checkBluetoothAddress ( endereco_MAC_do_Bluetooth_remoto ) ) {
					// atribui o valor do endereço de MAC para a variável dispositivoBluetoothRemoto
					dispositivoBluetoothRemoto =
							meuBluetoothAdapter.getRemoteDevice ( endereco_MAC_do_Bluetooth_remoto );
				} else {
					// exibe uma mensagem de erro
					
					ShowMSG ( "Endereço MAC do dispositivo Bluetooth remoto não é válido" );
				}
				
				try {
					// atribui o código UUID a variável bluetoothSocket
					bluetoothSocket =
							dispositivoBluetoothRemoto.createInsecureRfcommSocketToServiceRecord ( MEU_UUID );
					bluetoothSocket.connect ( ); // estabelece a conexão
					medicoesRecebidas.setText ( "" );
					
					ShowMSG ( "Conectado" );
				} catch ( IOException e ) {
					Log.e ( "ERRO AO CONECTAR", "O erro foi" + e.getMessage ( ) );
					
					ShowMSG ( "Conexão não foi estabelecida" );
				}
			} else {
				medicoesRecebidas.setText ( "" );
				if ( bluetoothSocket != null ) {
					try {
						// Fecha imediatamente o soquete e libera todos os recursos associados.
						bluetoothSocket.close ( ); // encerra a conexão
						bluetoothSocket = null;
						// exibe uma mensagem
						
						
						ShowMSG ( "Conexão encerrada" );
					} catch ( IOException e ) {
						Log.e ( "ERRO AO DESCONECTAR", "O erro foi" + e.getMessage ( ) );
						
						
						ShowMSG ( "Erro - A conexão permanece estabelecida" );
					}
				} else {
					ShowMSG ( "Não há nenhuma conexão estabelecida a ser desconectada" );
					
				}
			}
		} else if ( view.getId ( ) == R.id.btnConfig ) {
			AlertDialog.Builder alert = new AlertDialog.Builder ( this );
			alert.setTitle ( "Alterar MAC Address" );
			alert.setMessage ( "\nInforme o MAC Address no campo abaixo: " );
			
			final EditText macAddress = new EditText ( this );
			macAddress.setText ( endereco_MAC_do_Bluetooth_remoto );
			
			alert.setView ( macAddress );
			alert.setPositiveButton ( "Confirmar", new DialogInterface.OnClickListener ( ) {
				@Override
				public void onClick ( DialogInterface dialog, int which ) {
					
					endereco_MAC_do_Bluetooth_remoto = macAddress.getText ( ).toString ( );
					ShowMSG ( "MAC Address alterado com sucesso!" );
					conectar.setChecked ( false );
				}
			} );
			
			alert.setNegativeButton ( "Cancelar", new DialogInterface.OnClickListener ( ) {
				@Override
				public void onClick ( DialogInterface dialog, int which ) {
				
				}
			} );
			AlertDialog dialog = alert.create ( );
			dialog.show ( );
		} else if ( view.getId ( ) == R.id.btnMedirTemperatura ) {
			// Verifica se há conexão estabelecida com o Bluetooth.
			if ( bluetoothSocket != null ) {
				medicoesRecebidas.setText ( "" ); // limpa a caixa de texto
				// passa o id do botão acionado
				try {
					//Envia Temperatura através do Socket
					outStream = bluetoothSocket.getOutputStream ( );
					//sendData ( "t" );
					SystemClock.sleep ( 1000 );
					// Obtenha o fluxo de entrada associado a este soquete.
					inputStream = bluetoothSocket.getInputStream ( );
					
					// Lê bytes deste fluxo e os armazena num array de bytes
					byte[] msgBuffer = new byte[ 1024 ];
					inputStream.read ( msgBuffer );
					// exibe o valor na caixa de texto
					
					
					String result = new String ( msgBuffer );
					
					String result1 = result;
					result1 += "\n" + result;
					
					medicoesRecebidas.setText ( result1 );
					
				} catch ( IOException e ) {
					Log.e ( "ERROR", "O erro foi" + e.getMessage ( ) );
					ShowMSG ( "Mensagem não recebida" );
					
				}
				
			}
		} else {
			ShowMSG ( "Bluetooth não está conectado" );
			
		}
		
	}
	
	private void ShowMSG ( String msg ) {
		Snackbar.make ( linearLayout, msg, Snackbar.LENGTH_LONG ).show ( );
	}
}

