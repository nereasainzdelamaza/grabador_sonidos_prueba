package com.example.grabador_sonidos;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.media.AudioManager;

public class MainActivity extends Activity {

	//Parametros de grabacion de audio del movil
	private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav"; // Formato del archivo que grabo, para despues reproducir
    private static final String AUDIO_RECORDER_FILE_NAME_WAV = "sonido"; // Nombre del archivo que grabo, para despues reproducir
    private static final String AUDIO_RECORDER_FOLDER = "MisCuaqs"; // Nombre de la carpeta donde guardare ese archivo
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw"; // Nombre del archivo temporal donde guardo los bytes del microfono
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    
    //recorder es el encargado de instanciar el microfono y grabar
    private AudioRecord recorder = null; 
    private int bufferSize = 0;
    //recordingThread es un hilo encargado de ir grabando en el buffer lo que vamos escuchando por el microfono
    private Thread recordingThread = null;
  
    //Una variable para no empezar a grabar, si ya estamos grabando
    private boolean isRecording = false;

    //Es la clase encargada de leer un fichero del movil, y reproducirlo
    reproductor my_player;
    SoundManager snd;
	int water, alarm, fly, door, laughter, phone;
    
    //Estos son los controles de la pantalla, para habilitarlos, cambiarles el texto, etc...
	TextView TextViewEstadoMicrofono;
	TextView TextViewValorPitch;
	Button buttonGrabar;
	Button buttonPlay;
	Button buttonStopGrabar;
	Button buttonStopPlay;
	
	Button buttoneco;
	Button buttonreverb;
	Button buttonmetal;
	Button buttonchorus;
	Button buttonhelio;
	Button buttonpitufo;
	Button buttonmalvado;
	Button buttonfantasma;
		
	//Estos son los valores correspondientes al pitch
    float _valuePitch = 1f; // Es el valor que se le pasa al reproductor, para aplicar el efecto
    float _maxValuePitch = 2f; //En android el valor maximo de pitch es 2
    float _minValuePitch = 0.5f; //En android el valor minimo de pitch es 0.5
    float _stepPitch = 0.1f; // cada vez que pulsamos el boton + o - , cambiamos el valor del pitch en 0.1 (lo ajustamos a lo que queramos)
	

    //El onCreate es el primer metodo que se abre al iniciar la activity
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


        snd = new SoundManager(getApplicationContext());
 ////////////////////////////////////////////////////////////////////////////////
		// Set volume rocker mode to media volume
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
 
        // Load the samples from res/raw
        water = snd.load(R.raw.agua);
        alarm = snd.load(R.raw.alarma);
        fly = snd.load(R.raw.mosca);
        door = snd.load(R.raw.puerta);
        laughter = snd.load(R.raw.risa);
        phone = snd.load(R.raw.telefono);
           
	      
	   //obtengo el valor del buffer size en funcion de mis parametros de grabacion
       bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);

       //Relaciono mis variables de los controles, con los controles que hay en el layout de la activity
 	   TextViewEstadoMicrofono = (TextView)this.findViewById(R.id.tv_estado);
	   TextViewEstadoMicrofono.setText(getResources().getString(R.string.parado));
	   
	   TextViewValorPitch =  (TextView)this.findViewById(R.id.tv_value_pitch);
	   TextViewValorPitch.setText(String.valueOf(_valuePitch));
	   
	   buttonGrabar = (Button)this.findViewById(R.id.button_grabar);
	   buttonPlay = (Button)this.findViewById(R.id.button_play);
	   buttonStopGrabar = (Button)this.findViewById(R.id.button_stop_recording);
	   buttonStopPlay = (Button)this.findViewById(R.id.button_stop_playing);
	   
	   buttoneco= (Button)this.findViewById(R.id.button_echo);
	   buttonreverb= (Button)this.findViewById(R.id.button_reverb);
	   buttonmetal= (Button)this.findViewById(R.id.button_metal);
	   buttonchorus= (Button)this.findViewById(R.id.button_chorus);
	   buttonhelio= (Button)this.findViewById(R.id.button_helio);
	   buttonpitufo= (Button)this.findViewById(R.id.button_pitufo);
	   buttonmalvado= (Button)this.findViewById(R.id.button_malvado);
	   buttonfantasma= (Button)this.findViewById(R.id.button_fantasma);
	   
	   buttonPlay.setEnabled(false);
	   buttonStopGrabar.setEnabled(false);
	   
	   //Instancio mi reproductor de sonidos
	   my_player = new reproductor(this);
	   my_player.setPitchValue(_valuePitch);
	
	}

	//Este metodo 'a–ade' el menu a la activity, pero en este proyecto el menu no lo necesitamos para nada
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	//Metodo al que se llama cuando se pulsa el boton +
	public void subePitchClick(View v)
	{
		DecimalFormat df = new DecimalFormat("##.#");
		float nextValue = _valuePitch + _stepPitch;

		
		//Comprueba que el valor del _valuePitch no supere el maximo permitido
		if(nextValue<=_maxValuePitch )
		{
			_valuePitch = nextValue;
		}
		else
		{
			_valuePitch = _maxValuePitch;
		}

		//Pone el valor en el textView de la pantalla
		 TextViewValorPitch.setText(String.valueOf(df.format(_valuePitch)));
		 //Ajusta el valor del pitch en el reproductor
		 my_player.setPitchValue(_valuePitch);
	}
	
	//Metodo al que se llama cuando se pulsa el boton -
	public void bajaPitchClick(View v)
	{
		float nextValue = _valuePitch - _stepPitch;
		DecimalFormat df = new DecimalFormat("##.#");

		//Comprueba que el valor del _valuePitch no sea inferior al minimo permitido
		if(nextValue>=_minValuePitch)
		{
			_valuePitch = nextValue;
		}
		else
		{
			_valuePitch = _minValuePitch;
		}
		//Pone el valor en el textView de la pantalla
		 TextViewValorPitch.setText(String.valueOf(df.format(_valuePitch)));
		 //Ajusta el valor del pitch en el reproductor
		 my_player.setPitchValue(_valuePitch);
	}
	

	
	//Metodo al que se llama cuando se pulsa el boton 'Play'
	public void PlayClick(View v)
	{
		_valuePitch = 1;
		my_player.setPitchValue(_valuePitch);
   		this.playSoundRecorded();
	}
	
	//Metodo al que se llama cuando se pulsa el boton 'Parar de reproducir'
	public void stopPlayingClick(View v)
	{
		   	buttonGrabar.setEnabled(true);
			this.stopSoundRecorded();
	}
	
	//Metodo al que se llama cuando se pulsa el boton 'Grabar'
	public void RecordClick(View v)
	{
		if(!isRecording)
		{
			isRecording=true;
	    	TextViewEstadoMicrofono.setText(getResources().getString(R.string.grabando));;
	    	this.startRecording();
	    	buttonStopGrabar.setEnabled(true);	
	    	buttonGrabar.setEnabled(false);
		}

	}
	
	//Metodo al que se llama cuando se pulsa el boton 'Parar de grabar'
	public void stopRecordingClick(View v)
	{
		if(isRecording)
		{
			isRecording = false;
	    	this.stopRecording();
	       	TextViewEstadoMicrofono.setText(getResources().getString(R.string.parado));
	       	buttonPlay.setEnabled(true);
	    	buttonGrabar.setEnabled(true);
	    	buttonStopGrabar.setEnabled(false);	
		}
	}
	
	// Metodo al que se llama cuando se pulsa el boton 'Eco' 
		//6400 sonaba tipo reverb, 16400 también suena un poco
		//En el otro era 16000 --> 1600*4= 6 
		//Proporcionalmente 44100 --> 4410 *4 = 17640
		//Aquí el samplerate es 44100
		public void echoClick(View v)
		{
			int delay = 18000; //17640
	        float attenuation = 0.5f; 
	        this.myEffect(delay, attenuation);
		}
		
		//2000 ---- 16000
		//x    ---- 44100 x= 5512
		public void reverbClick(View v)
		{
			int delay = 6000; //5512
            float attenuation = 0.5f;
            this.myEffect(delay, attenuation);
			
		}
		
		public void metalClick(View v)
		{			
            this.myMetal();			
		}
		
		public void chorusClick(View v)
		{	
		/*	//Para probar otra reverb
			int delay = 8000;
            float attenuation = 0.5f;
            this.myEffect(delay, attenuation);*/
            //this.myChorus();	
			//this.myPrueba8(); // Al revés
			//this.myPrueba9();
			this.myPrueba7();
			//snd.play(fly);
 		}
		
		public void helioClick(View v)
		{	
			_valuePitch = 1.4f;
			my_player.setPitchValue(_valuePitch);
			this.playSoundRecorded();
		}
		
		public void pitufoClick(View v)
		{	
			_valuePitch = 1.8f;
			my_player.setPitchValue(_valuePitch);
			this.playSoundRecorded();
		}
		
		public void malvadoClick(View v)
		{	
			_valuePitch = 0.8f;
			my_player.setPitchValue(_valuePitch);
			this.playSoundRecorded();
		}
		
		public void fantasmaClick(View v)
		{	
			_valuePitch = 0.6f;
			my_player.setPitchValue(_valuePitch);
			this.playSoundRecorded();
		}
		
		
		
	//Nombre del fichero .wav que guardaremos para pode reproducirlo despues
	 private String getFilename()
	 {
		    String filepath = Environment.getExternalStorageDirectory().getPath(); // Obtengo la direcci—n de la carpeta SD del movil
	        File file = new File(filepath,AUDIO_RECORDER_FOLDER); // Le a–ado el nombre de la carpeta donde quiero guardarlo
	        
	        if(!file.exists()){ 
	                file.mkdirs(); //Si esa carpeta no existe, la crea
	        }
	        
	        //Devuelve el nombre del fichero, que es = Direccion SD + nombre de la carpeta + nombre fichero + extension wav
	        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_FILE_NAME_WAV + AUDIO_RECORDER_FILE_EXT_WAV);
	 }
	 

	//Nombre del fichero temporal en el que vamos almacenando los bytes recogidos por el microfono
	 private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath(); // Obtengo la direcci—n de la carpeta SD del movil
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);  // Le a–ado el nombre de la carpeta donde quiero guardarlo
        
        if(!file.exists()){
                file.mkdirs(); //Si esa carpeta no existe, la crea
        }
        
        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE); //Nombre del fichero temporal que quiero guardar
        
        if(tempFile.exists())
                tempFile.delete(); // si esta, lo borra
        
        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE); //Devuelve el nombre del fichero temporal
	 }
	 
		//Este metodo coge la instancia de la clase reproductor, y le dice que abra el fichero wav que hemos creado
		private void playSoundRecorded()
		{
			my_player.PlayCuaq(this.getFilename());
		}
		
		//Este metodo coge la instancia de la clase reproductor y le dice que pare de reproducir 
		private void stopSoundRecorded()
		{
			my_player.stopPlaying();
		}
	 
		
		
	 private void startRecording(){
			
		
		try{
			//instancio el microfono y el grabador
	        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
	                                        RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);
	        //comienzo a grabar
	        recorder.startRecording();
	        isRecording = true;

	        //instancio el hilo que se encarga de ir llamando todo el rato al metodo writeAudioDataToFile() 
	        //(lo que hacia el buffer_ready en windows phone. O sea ir grabando los datos que lee del microfono 
	        //en algun lado para no perderlos
	    	recordingThread = new Thread(new Runnable() {
	                
	                @Override
	                public void run() {
	                        writeAudioDataToFile();
	                }
	        },"AudioRecorder Thread");
	        
	    	//Pongo en marcha el hilo que se encarga de guardar lo escuchado en algun lado
	        recordingThread.start();
		}catch(Exception ex)
		{
			recorder.release();
			Log.e("miApp","Error " + ex.getLocalizedMessage());
			Toast.makeText(this, getResources().getString(R.string.error_micro), Toast.LENGTH_LONG).show();

		}
	}


	//A este metodo se le llama muchas veces mientras grabamos por el microfono
	//Es como el buffer_ready en windows phone, que se llamaba muchas veces
	//para ir volcando la informacion que se escuchaba
	private void writeAudioDataToFile(){
		
			Log.i("miApp","Entro en writeAudioDataToFile");
		
	        byte data[] = new byte[bufferSize];
	        
	       //Declaro el path del archivo temporal donde quiero guardar los bytes 'escuchados por el microfono'
	        String filename = getTempFilename();
	        FileOutputStream os = null;
	        
	        try {
	                os = new FileOutputStream(filename);
	        } catch (FileNotFoundException e) {
	                e.printStackTrace();
	        }
	        
	        int read = 0;
	        
	
	        if(null != os){
	                while(isRecording){
	                	
	                	//Esta instrucci—n lee lo que hay en el recorder, y mete los datos en el array de bytes llamado sData
	                        read = recorder.read(data, 0, bufferSize);
	                        
	                        if(AudioRecord.ERROR_INVALID_OPERATION != read){
	                                try {
	                                	//Guarda los bytes en el fichero os
	                                        os.write(data);
	                                } catch (IOException e) {
	                                        e.printStackTrace();
	                                }
	                        }
	                }
	                
	                try {
	                        os.close();
	                } catch (IOException e) {
	                        e.printStackTrace();
	                }
	        }
	}

	private void stopRecording(){
		
	        if(null != recorder){
	        	//Cierra el grabador y para el hilo de lectura de bytes del microfono
	                isRecording = false;
	                
	                recorder.stop();
	                recorder.release();
	                
	                recorder = null;
	                recordingThread = null;
	        }
	        
	        
	        //ESTE METODO DEVUELVE TODOS LOS BYTES LEIDOS POR EL MICROFONO
	        //ESTOS SON LOS BYTES QUE SE PUEDEN MANIPULAR PARA CREAR EFECTOS
	        this.getBytesFromFileRecorded();
	 
	        
	        //Este metodo coge todos los bytes escuchados por el microfono, y crea un archivo wav (con su cabecera etc...)
	        //para poder luego reproducirlo con el movil
	        copyWaveFile(getTempFilename(),getFilename());
	        
	        //Por ultimo, borra el fichero temporal, porque ya no lo necesitamos
	        deleteTempFile();
	}
	


	
	//OBTIENE LOS BYTES ESCUCHADOS POR EL MICROFONO
	private byte[] getBytesFromFileRecorded()
	{
		String path = this.getTempFilename();
		File file = new File(path);
	    int size = (int) file.length();
	    byte[] bytes = new byte[size];
	    try {
	        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
	        buf.read(bytes, 0, bytes.length);
	        buf.close();
	    } catch (FileNotFoundException e) {

	    	e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    
	    Log.i("miApp","Los bytes del archivo grabado tienen una longitud de "+ bytes.length);
	    return bytes;
	}


	//Borra el fichero temporal
	private void deleteTempFile() {
	        File file = new File(getTempFilename());
	        file.delete();
	}

	//Crea el archivo wav
	private void copyWaveFile(String inFilename,String outFilename){
	        FileInputStream in = null;
	        FileOutputStream out = null;
	        long totalAudioLen = 0;
	        long totalDataLen = totalAudioLen + 36;
	        long longSampleRate = RECORDER_SAMPLERATE;
	        int channels = 2;
	        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;
	        
	        byte[] data = new byte[bufferSize];
	        
	        try {
	                in = new FileInputStream(inFilename);
	                out = new FileOutputStream(outFilename);
	                totalAudioLen = in.getChannel().size();
	                totalDataLen = totalAudioLen + 36;
	                
	           
	                
	                WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
	                                longSampleRate, channels, byteRate);
	                
	                while(in.read(data) != -1){
	                        out.write(data);
	                }
	                
	                in.close();
	                out.close();
	        } catch (FileNotFoundException e) {
	                e.printStackTrace();
	        } catch (IOException e) {
	                e.printStackTrace();
	        }
	}

	//Crea la cabecera del archivo wav
	private void WriteWaveFileHeader(
	                FileOutputStream out, long totalAudioLen,
	                long totalDataLen, long longSampleRate, int channels,
	                long byteRate) throws IOException {
	        
	        byte[] header = new byte[44];
	        
	        header[0] = 'R';  // RIFF/WAVE header
	        header[1] = 'I';
	        header[2] = 'F';
	        header[3] = 'F';
	        header[4] = (byte) (totalDataLen & 0xff);
	        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
	        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
	        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
	        header[8] = 'W';
	        header[9] = 'A';
	        header[10] = 'V';
	        header[11] = 'E';
	        header[12] = 'f';  // 'fmt ' chunk
	        header[13] = 'm';
	        header[14] = 't';
	        header[15] = ' ';
	        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
	        header[17] = 0;
	        header[18] = 0;
	        header[19] = 0;
	        header[20] = 1;  // format = 1
	        header[21] = 0;
	        header[22] = (byte) channels;
	        header[23] = 0;
	        header[24] = (byte) (longSampleRate & 0xff);
	        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
	        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
	        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
	        header[28] = (byte) (byteRate & 0xff);
	        header[29] = (byte) ((byteRate >> 8) & 0xff);
	        header[30] = (byte) ((byteRate >> 16) & 0xff);
	        header[31] = (byte) ((byteRate >> 24) & 0xff);
	        header[32] = (byte) (2 * 16 / 8);  // block align
	        header[33] = 0;
	        header[34] = RECORDER_BPP;  // bits per sample
	        header[35] = 0;
	        header[36] = 'd';
	        header[37] = 'a';
	        header[38] = 't';
	        header[39] = 'a';
	        header[40] = (byte) (totalAudioLen & 0xff);
	        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
	        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
	        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

	        out.write(header, 0, 44);
	}
	
	private short[] byte2short(byte[] src) {
	    short[] dest = new short[src.length / 2];
	   // ByteBuffer.wrap(src).asShortBuffer().get(dest);
	    ByteBuffer.wrap(src).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(dest);
	    return dest;
	}
	
	private byte[] short2byte(short[] sData) {
	    int shortArrsize = sData.length;
	    byte[] bytes = new byte[shortArrsize * 2];
	    for (int i = 0; i < shortArrsize; i++)
	    {
	        bytes[i * 2] = (byte) (sData[i] & 0x00FF);
	        bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
	        sData[i] = 0;
	    }
	    return bytes;
	}	
	
		
	private void myEffect(int delay, float attenuation) {
		
	//	byte [] voice = this.getBytesFromFileRecorded(); // no recoge nada
	
	//	byte [] voice = null;
	//	voice = this.getBytes();
		
        String path = this.getFilename();
		File file = new File(path);						
	    int size = (int) file.length(); 
	    byte[] voice = new byte[size];
	
 
	    try {
	        BufferedInputStream bif = new BufferedInputStream(new FileInputStream(file));
	        bif.read(voice, 0, voice.length);
	        bif.close();
	    } catch (FileNotFoundException e) {

	    	e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }	         
		
		//Convert from byte to short
		short[] samples = new short[(voice.length / 2) + delay];
		samples = byte2short(voice);
		
		
		// Save modifiedsamples
	    short [] samplesmod = samples;
	    	       	
		for (int i = 0; i < samples.length - delay; i++)
        {
			samplesmod[i + delay] += (short)((float)samples[i] * attenuation);
        }
		
		// Convert short array to byte array
		byte [] voicemod = new byte[voice.length + delay*2];	    
	    voicemod = short2byte(samplesmod);
		
		String patheffect = "/storage/sdcard0/MisCuaqs/effect.wav";
		File fileeffect = new File (patheffect);
		try 
		{
			BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(fileeffect));
	        bof.write(voicemod, 0, voicemod.length);
	        bof.close();
		} catch (FileNotFoundException e) {

	    	e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }		       
		  my_player.PlayCuaq(patheffect);	  
	}
	
	// Aquí se vuelve loco
	//160, 320, 480, valores viejos
	private void myMetal() {
		
		int delay1 = 640; 
        int delay2 = 1280;
        int delay3 = 1920;
        float attenuation1 = 0.4f;
        float attenuation2 = 0.5f;
        float attenuation3 = 0.6f;
		
		String path = this.getFilename();
		File file = new File(path);						
	    int size = (int) file.length(); 
	    byte[] voice = new byte[size];
	
	    try {
	        BufferedInputStream bif = new BufferedInputStream(new FileInputStream(file));
	        bif.read(voice, 0, voice.length);
	        bif.close();
	    } catch (FileNotFoundException e) {
	
	    	e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }	         
		
		//Convert from byte to short
		short[] samples = new short[(voice.length / 2) + delay1 + delay2 + delay3];
		samples = byte2short(voice);
		
		
		// Save modifiedsamples
	    short [] samplesmod = samples;
		
	    for (int i = 0; i < samples.length - delay1; i++)
        {
            samplesmod[i + delay1] += (short)((float)samples[i] * attenuation1);
        }

        for (int i = 0; i < samples.length - delay1 - delay2 - delay3; i++)
        {
            samplesmod[i + delay2] += (short)((float)samples[i] * attenuation2);
        }

        for (int i = 0; i < samples.length - delay1 - delay2 - delay3; i++)
        {
            samplesmod[i + delay3] += (short)((float)samples[i] * attenuation3);
        }
	    
        byte[] voicemod = new byte[voice.length + delay1 * 2 + delay2 * 2 + delay3 * 2];
        voicemod = short2byte(samplesmod);
		
		String patheffect = "/storage/sdcard0/MisCuaqs/effect.wav";
		File fileeffect = new File (patheffect);
		try 
		{
			BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(fileeffect));
	        bof.write(voicemod, 0, voicemod.length);
	        bof.close();
		} catch (FileNotFoundException e) {

	    	e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }		
		  //copyWaveFile(path,patheffect);        
		  my_player.PlayCuaq(patheffect);	  
         
	}


private void myChorus() {
		
		String path = this.getFilename();
		File file = new File(path);						
		int size = (int) file.length(); 
		byte[] voice = new byte[size];
		
		try {
		        BufferedInputStream bif = new BufferedInputStream(new FileInputStream(file));
		        bif.read(voice, 0, voice.length);
		        bif.close();
		    } catch (FileNotFoundException e) {
		
		    	e.printStackTrace();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }	         
			
		//Convert from byte to short
		short[] samples = new short[(voice.length / 2)];
		samples = byte2short(voice);
					
		// Save modifiedsamples
		// short [] samplesmod = new short[samples.length];
		short [] samplesmod = samples;
		
		///////////
		///////////
		/////////// EFECTO CORO
		///////////
		///////////
		///////////
		
		byte[] voicemod = new byte[voice.length*2];
        voicemod = short2byte(samplesmod);
    		
    	String patheffect = "/storage/sdcard0/MisCuaqs/effect.wav";
    	File fileeffect = new File (patheffect);
    	try 
    	{
    		BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(fileeffect));
    	    bof.write(voicemod, 0, voicemod.length);
    	    bof.close();
    	} catch (FileNotFoundException e) {

    	    e.printStackTrace();
    	} catch (IOException e) {
    	    e.printStackTrace();
    	}		     
    	
    	my_player.PlayCuaq(patheffect);
	}


////////////////// PRUEBA1 : ruido
	private void myPrueba1() 
	{
		String path = this.getFilename();
		File file = new File(path);						
		int size = (int) file.length(); 
		byte[] voice = new byte[size];
	
		try {
		        BufferedInputStream bif = new BufferedInputStream(new FileInputStream(file));
		        bif.read(voice, 0, voice.length);
		        bif.close();
		    } catch (FileNotFoundException e) {
	
		    	e.printStackTrace();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }	         
			
		//Convert from byte to short
		short[] samples = new short[(voice.length / 2)];
		samples = byte2short(voice);
					
		// Save modifiedsamples
		// short [] samplesmod = new short[samples.length];
		short [] samplesmod = samples;
	
		
		float attenuation = 0.8f; 
		double sampleRate = 44100.0;		
		double seconds = 2.0;
		double f0 = 440.0;
		double amplitude0 = 0.8;
		double twoPiF0 = 2 * Math.PI * f0;
		double f1 = 6 * f0;
		double amplitude1 = 0.2;
		double twoPiF1 = 2 * Math.PI * f1;		
		//float[] buffer = new float[(int) (seconds * sampleRate)];
		float[] buffer = new float[samples.length];
	
		for (int sample = 0; sample < buffer.length; sample++) 
		{
			double time = sample / sampleRate;
			double f0Component = amplitude0 * Math.sin(twoPiF0 * time);
			double f1Component = amplitude1 * Math.sin(twoPiF1 * time);
			buffer[sample] = (float) (f0Component + f1Component);
		}
	
		//Converting floats to bytes
		final byte[] byteBuffer = new byte[buffer.length * 2];
		int bufferIndex = 0;
		for (int i = 0; i < byteBuffer.length; i++) 
		{
			final int x = (int) (buffer[bufferIndex++] * 32767.0);
			byteBuffer[i] = (byte) x;
			i++;
			byteBuffer[i] = (byte) (x >>> 8);
		}
	
		//int max = Integer.MAX_VALUE;
		int max = byteBuffer[0];
		for(int i=0; i<byteBuffer.length; i++)
		{
			if(byteBuffer[i] > max)
			{
				max = byteBuffer[i];
			}
		}
	
		for (int i = 0; i < samples.length-44-max; i++)
		{
			// samplesmod[i+44]=byteBuffer[i];
			// samplesmod[i+44] = (short)((float)samples[i+44] + samples[Math.abs(i+44 - byteBuffer[i])] * attenuation);
			//samplesmod[i+44] += (short)(samples[Math.abs(i+44 - byteBuffer[i])]);
			samplesmod[i+44] += (short)(samples[Math.abs(i+44 + byteBuffer[i])]);
			    
		}
		
		// Si pongo el valor absoluto no hace falta hacer el if and else
		
		  // if (byteBuffer[i] >= 0 && byteBuffer[i] < i)
			// samplesmod[i+44] = (short)((float)samples[i+44] + samples[i+44 - byteBuffer[i]] * attenuation);
		   	
		 //   else
			//	samplesmod[i+44] = (short)((float)samples[i+44] + samples[Math.abs(i+44 + byteBuffer[i])] * attenuation);
		    
		
	
		byte[] voicemod = new byte[voice.length*2];
		voicemod = short2byte(samplesmod);
			
		String patheffect = "/storage/sdcard0/MisCuaqs/effect.wav";
		File fileeffect = new File (patheffect);
		try 
		{
			BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(fileeffect));
		    bof.write(voicemod, 0, voicemod.length);
		    bof.close();
		} catch (FileNotFoundException e) {
	
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		}		     
	
		my_player.PlayCuaq(patheffect);
}
		
	

//////////////////PRUEBA2
private void myPrueba2() 
{
	String path = this.getFilename();
	File file = new File(path);						
	int size = (int) file.length(); 
	byte[] voice = new byte[size];
	
	try {
	BufferedInputStream bif = new BufferedInputStream(new FileInputStream(file));
	bif.read(voice, 0, voice.length);
	bif.close();
	} catch (FileNotFoundException e) {
	
	e.printStackTrace();
	} catch (IOException e) {
	e.printStackTrace();
	}	         
	
	//Convert from byte to short
	short[] samples = new short[(voice.length / 2)];
	samples = byte2short(voice);
	
	// Save modifiedsamples
	// short [] samplesmod = new short[samples.length];
	short [] samplesmod = samples;
			
	double sampleRate = 44100.0;
	double frequency = 88200; //440 //Con 44100 se escucha amplificado, 441 se escucha como radio, 8000, 16000, 32000
	double amplitude = 0.8;
	//double seconds = 2.0;
	double twoPiF = 2 * Math.PI * frequency;
	//float[] buffer = new float[(int) (seconds * sampleRate)];
	float[] buffer = new float[samples.length];
	for (int sample = 0; sample < buffer.length; sample++) 
	{
		double time = sample / sampleRate;
		buffer[sample] = (float) ((float) amplitude * Math.sin(twoPiF * time));
	}
	
	byte[] voicemod = new byte[voice.length*2];
	voicemod = short2byte(samplesmod);
		
	String patheffect = "/storage/sdcard0/MisCuaqs/effect.wav";
	File fileeffect = new File (patheffect);
	try 
	{
		BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(fileeffect));
	    bof.write(voicemod, 0, voicemod.length);
	    bof.close();
	} catch (FileNotFoundException e) {

	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}		     

	my_player.PlayCuaq(patheffect);
}
	


/////////// Tengo que disminuir el máximo y que sea periódico, probar con las mezclas del otro día
private void myPrueba3() 
{
		String path = this.getFilename();
		File file = new File(path);						
		int size = (int) file.length(); 
		byte[] voice = new byte[size];
		
		try {
		        BufferedInputStream bif = new BufferedInputStream(new FileInputStream(file));
		        bif.read(voice, 0, voice.length);
		        bif.close();
		    } catch (FileNotFoundException e) {
		
		    	e.printStackTrace();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }	         
			
		//Convert from byte to short
		short[] samples = new short[(voice.length / 2)];
		samples = byte2short(voice);
					
		// Save modifiedsamples
		// short [] samplesmod = new short[samples.length];
		short [] samplesmod = samples;
		
		float attenuation = 0.8f; 
		short[] lfo = new short[samples.length];
        double amplitude = 0.05 * Short.MAX_VALUE; //0.15 * Short.MAX_VALUE
        double frequency = 0.2; //0.2

        for (int i = 40000; i < lfo.length+40000; i++)
        {
            //lfo[i] = (short)(amplitude * Math.sin((2 * Math.PI * i * frequency) ));
            lfo[i-40000] = (short) (amplitude* 2 * Math.PI *i* frequency/RECORDER_SAMPLERATE); 
        }
       

        int max = lfo[0];
		for(int i=0; i<lfo.length; i++)
		{
			if(lfo[i] > max)
			{
				max = lfo[i];
			}
		} 


        for (int i = 0; i < samples.length-44-max; i++)
        {

        	// Sólo se escucha un pitidillo y luego al final el orginal porque  por la resta no hace el bucle completo
        	//samplesmod[i+44] = (short)((samples[Math.abs(i - lfo[i])]) * attenuation); 
        	
        	samplesmod[i+44] += (short)((samples[Math.abs(i - lfo[i])]) * attenuation);         	
        	
        	// Sólo ruido
        	//samplesmod[i+44] += (short)((float)samples[i] +(samples[Math.abs(i - lfo[i])]) * attenuation);  
        	
        	/*
            if (lfo[i] >= 0 && lfo[i] < i)
            { 
                // samplesmod[i] = (short)((float)samples[i] + samples[i - lfo[i]]);
                samplesmod[i+44] = (short)((float)samples[i] + samples[i - lfo[i]] * attenuation);
           	 //samplesmod[i+44] = (short)(samples[i - lfo[i]] * attenuation);
                
            }
            else
            {
                // samplesmod[i] = (short)((float)samples[i] + samples[i + lfo[i]]);
               samplesmod[i+44] = (short)((float)samples[i] + samples[i + lfo[i]] * attenuation);
           	 //samplesmod[i+44] = (short)(samples[i + lfo[i]] * attenuation); 
                
            }*/
        }
		
		byte[] voicemod = new byte[voice.length*2];
        voicemod = short2byte(samplesmod);
    		
    	String patheffect = "/storage/sdcard0/MisCuaqs/effect.wav";
    	File fileeffect = new File (patheffect);
    	try 
    	{
    		BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(fileeffect));
    	    bof.write(voicemod, 0, voicemod.length);
    	    bof.close();
    	} catch (FileNotFoundException e) {

    	    e.printStackTrace();
    	} catch (IOException e) {
    	    e.printStackTrace();
    	}		     
    	
    	my_player.PlayCuaq(patheffect);
	}

private void myPrueba4() 
{		
		String path = this.getFilename();
		File file = new File(path);						
		int size = (int) file.length(); 
		byte[] voice = new byte[size];
		
		try {
		        BufferedInputStream bif = new BufferedInputStream(new FileInputStream(file));
		        bif.read(voice, 0, voice.length);
		        bif.close();
		    } catch (FileNotFoundException e) {
		
		    	e.printStackTrace();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }	         
			
		//Convert from byte to short
		short[] samples = new short[(voice.length / 2)];
		samples = byte2short(voice);
					
		// Save modifiedsamples
		// short [] samplesmod = new short[samples.length];
		short [] samplesmod = samples;
		
//		Nuevo LFO 
/*			f=0.25;
			delay_in_sampels=0.05*fs;%50 ms delay
			index=1:length(x);
			sincurve = 1*sin(2*pi*index*f);
			y = zeros(length(x):1);
			y(1:delay_in_sampels)=x(1:delay_in_sampels);
			a=1;
			b=5;
			for i =(delay_in_sampels+1):length(x)
			    sink=abs(sincurve(i));
			    delay=(delay_in_sampels+sink);
			      o=ceil(i-delay);
			      y(i)=(a*x(i)) + b*(x(o)); 
			  end
			  soundsc(y,fs)
*/		
        // Siempre me sale de máximo 32767, pero a veces cambia más rápido que otras
			float attenuation = 0.8f;
			double frec= 0.25; 		//0.2 y 0.25 no me influye para los valores // Con 2 excepción
			//int delay = (int) (0.05 * RECORDER_SAMPLERATE); // Delay 50 ms
			short [] lfo= new short [samples.length];
	        double amplitude = 0.05 * Short.MAX_VALUE; 		//0.15 * Short.MAX_VALUE 		// 0.5 * Short.MAX_VALUE;
	        
	        //Con amplitude = 0.05*... baja a 10.000 el max
	        // Con  0.5, 5, 15 aquí empieza a hacer algo raro,
	        // Con 10, 150 sale de la app
	        
	        
			for (int i=0; i < lfo.length; i++)
			{
				// Probar del viejo
	            //lfo[i] = (short) (amplitude* 2 * Math.PI *i* frec/RECORDER_SAMPLERATE);
				lfo[i] = (short) (amplitude * 2 * Math.PI *i* frec/RECORDER_SAMPLERATE);
				
	            //Da lo mismo absoluto que nada --> ESto solo me saca 2 valores
				//lfo[i]= (short) Math.abs(((float)amplitude*Math.sin((2*Math.PI*frec*i))));
				 
			}
			
	        int max = lfo[0];
	        
			for(int i=0; i<lfo.length; i++)
			{
				if(lfo[i] > max)
				{
					max = lfo[i];
				}
			} 
			
	        for (int i = 0; i < samples.length-44-max; i++)
	        {
	        	 samplesmod[i+44] += (short)(Math.abs(samples[i - lfo[i]]) * attenuation);
	        }	 
	/*		
			//Darle una vuelta a lo del delay
			 for(int i=0; i<(samplesmod.length)-44-delay; i++)
		        {        	
			        	if (lfo[i] >= 0 && lfo[i] < i)
			            {
			        		//samplesmod[i+44] = (short) ((float) samples [i+44+7000]/2);  //sale de la app
				       		//samplesmod[i+44] += (short)samples[i-lfo[i]]* attenuation;
			        		//samplesmod[i+44+delay] += (short)samples[i-lfo[i]]* attenuation;
				       		
			        		//samplesmod[i+44] += (short)samples[i-lfo[delay]]* attenuation;
			            }
			            else
			           {
			         // 	 samplesmod[i+44] += (short)samples[i+lfo[i]]* attenuation;
			           }
		        }
		*/
		
		byte[] voicemod = new byte[voice.length*2];
        voicemod = short2byte(samplesmod);
    		
    	String patheffect = "/storage/sdcard0/MisCuaqs/effect.wav";
    	File fileeffect = new File (patheffect);
    	try 
    	{
    		BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(fileeffect));
    	    bof.write(voicemod, 0, voicemod.length);
    	    bof.close();
    	} catch (FileNotFoundException e) {

    	    e.printStackTrace();
    	} catch (IOException e) {
    	    e.printStackTrace();
    	}		     
    	
    	my_player.PlayCuaq(patheffect);
	}

private void myPrueba5() {
	
	String path = this.getFilename();
	File file = new File(path);						
	int size = (int) file.length(); 
	byte[] voice = new byte[size];
	
	try {
	        BufferedInputStream bif = new BufferedInputStream(new FileInputStream(file));
	        bif.read(voice, 0, voice.length);
	        bif.close();
	    } catch (FileNotFoundException e) {
	
	    	e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }	         
		
	//Convert from byte to short
	short[] samples = new short[(voice.length / 2)];
	samples = byte2short(voice);
				
	// Save modifiedsamples
	// short [] samplesmod = new short[samples.length];
	short [] samplesmod = samples;
	
	/*
	function Y = chorus(X,fs)
	f = 0.25;
	t0 = 0.05; % 50 ms delay
	tau = [zeros(size(X))];
	a = 0.2; b = 0.8; % abs(a) and abs(b) should sum up to 1
	A = t0*fs*0.99; % amplitude of delay variation should be below the initial delay
	N = length(X)
	for k = 1:N
	      tau(k) = round(t0*fs + A*sin(2*pi*f*k)); % values in tau have to be integers.
	  end
	tau = [zeros(1,tau(1)) X']; % to keep index above zero
	Y = [];
	for k = 1:length(X)
	      Y = [Y a*X(k)+b*X(round(k-tau(k)))];
	end
	*/
	
	double frec = 2; // 0.25 Hz // Con 2 suena como robot
	double tiempo = 0.005; //50 ms de delay : 0.05  	// Con 0.5 sale // Co 0.005 rudio raro
	double amplitude = tiempo * RECORDER_SAMPLERATE * 0.99;
	short [] lfo= new short [samples.length];
	double att1 = 0.7; //0.2
	double att2 = 0.3; //0.8
	for (int i=0; i < lfo.length; i++)		
	{	
	//lfo[i] = (short) (Math.round(tiempo * RECORDER_SAMPLERATE + amplitude*Math.sin(2*Math.PI*frec*i)));     
	//lfo[i] = (short) (Math.round(amplitude*Math.sin(2*Math.PI*frec*i)));     // sale de la aplicación
	
		lfo[i] = (short) ((short)20*(Math.round(tiempo * RECORDER_SAMPLERATE + amplitude*Math.sin(2*Math.PI*frec*tiempo*i))));     
	}
	
	 for (int i = 0; i < samples.length-44; i++)
     {
     	 samplesmod[i+44] += (short)(att1*samples[i+44] + att2 * samples[Math.abs(Math.round(i-lfo[i]))] );
     	 // Sin el más no hace nada
     }	
	
	 
	 
	byte[] voicemod = new byte[voice.length*2];
    voicemod = short2byte(samplesmod);
		
	String patheffect = "/storage/sdcard0/MisCuaqs/effect.wav";
	File fileeffect = new File (patheffect);
	try 
	{
		BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(fileeffect));
	    bof.write(voicemod, 0, voicemod.length);
	    bof.close();
	} catch (FileNotFoundException e) {

	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}		     
	
	my_player.PlayCuaq(patheffect);
}

private void myPrueba6() {
	
	String path = this.getFilename();
	File file = new File(path);						
	int size = (int) file.length(); 
	byte[] voice = new byte[size];
	
	try {
	        BufferedInputStream bif = new BufferedInputStream(new FileInputStream(file));
	        bif.read(voice, 0, voice.length);
	        bif.close();
	    } catch (FileNotFoundException e) {
	
	    	e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }	         
		
	//Convert from byte to short
	short[] samples = new short[(voice.length / 2)];
	samples = byte2short(voice);
				
	// Save modifiedsamples
	// short [] samplesmod = new short[samples.length];
	short [] samplesmod = samples;
	
	
	int delay = 18000;
	float attenuation = 0.4f;		
	for (int i = 0; i < samples.length - delay; i++)
    {
		//samplesmod[i] += (samples[i]*2);
		samplesmod[i + delay] += (short)((float)samples[i] * attenuation);
		//samplesmod[i] += (samples[i]*2);
    }
/*	
	for (int i = 0; i < samples.length; i++)
    {
		samplesmod[i] += (short)((float)samples[i]);
    }
*/    
	
	byte[] voicemod = new byte[voice.length*2];
    voicemod = short2byte(samplesmod);
		
	String patheffect = "/storage/sdcard0/MisCuaqs/effect.wav";
	File fileeffect = new File (patheffect);
	try 
	{
		BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(fileeffect));
	    bof.write(voicemod, 0, voicemod.length);
	    bof.close();
	} catch (FileNotFoundException e) {

	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}		     
	
	my_player.PlayCuaq(patheffect);
}


// Lo que se aproximaba pero no tiene en cuenta el lfo
private void myPrueba7() {
	
	String path = this.getFilename();
	File file = new File(path);						
	int size = (int) file.length(); 
	byte[] voice = new byte[size];
	
	try {
	        BufferedInputStream bif = new BufferedInputStream(new FileInputStream(file));
	        bif.read(voice, 0, voice.length);
	        bif.close();
	    } catch (FileNotFoundException e) {
	
	    	e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }	         
		
	//Convert from byte to short
	short[] samples = new short[(voice.length / 2)];
	samples = byte2short(voice);
				
	// Save modifiedsamples
	// short [] samplesmod = new short[samples.length];
	short [] samplesmod = samples;
	
	//El eco original
	/*
	for (int i = 0; i < samples.length - 8000; i++)
    {
		samplesmod[i + 8000] += (short)((float)samples[i] * 0.8);
    }
    */
	
	//Justo al revés
	/*for(int i=0; i<(samplesmod.length)-44-10000; i++) //4000 //8000
	{ 
      	samplesmod[i+44] += (short) ((float) samples [i+44+10000]*0.5);        	     	
    }*/
	

	//Lo que más se parece
	for(int i=0; i<(samplesmod.length)-44-16000; i++) //4000 //8000
	{ 
      	samplesmod[i+44] += (short) ((float) samples [i+44+10000]*0.7 +  samples [i+44+16000]*0.4);        	     	
    }
    
	 
	
	/* Random r = new Random();

	 int[] randomnum = new int [samples.length];
	 for (int i=0; i<randomnum.length; i++)
	 {
		 randomnum[i] = r.nextInt(8000);
	 }
	 
	 for (int i = 0; i < samples.length-44; i++)
     {
     	 samplesmod[i+44] = (short)(samples[i+44] + samples[Math.abs(Math.round(i-randomnum[i]))]);
     }	
	 */
	
	
	byte[] voicemod = new byte[voice.length*2];
    voicemod = short2byte(samplesmod);
		
	String patheffect = "/storage/sdcard0/MisCuaqs/effect.wav";
	File fileeffect = new File (patheffect);
	try 
	{
		BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(fileeffect));
	    bof.write(voicemod, 0, voicemod.length);
	    bof.close();
	} catch (FileNotFoundException e) {

	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}		     
	
	my_player.PlayCuaq(patheffect);
}

//Reproducir al revés
private void myPrueba8() {
	
	String path = this.getFilename();
	File file = new File(path);						
	int size = (int) file.length(); 
	byte[] voice = new byte[size];
	
	try {
	        BufferedInputStream bif = new BufferedInputStream(new FileInputStream(file));
	        bif.read(voice, 0, voice.length);
	        bif.close();
	    } catch (FileNotFoundException e) {
	
	    	e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }	         
		
	//Convert from byte to short
	short[] samples = new short[(voice.length / 2)];
	samples = byte2short(voice);
				
	// Save modifiedsamples
	 short [] samplesmod = new short[samples.length];
	//short [] samplesmod = samples;
	
	 for (int i = 0; i < 43; i++)
	    {
	      samplesmod[i] = samples [i];
	    }
	 
	
    for (int i = 0; i < samples.length-44; i++)
    {
      samplesmod[i+ 44] = (short)((float)samples[(samples.length - i -1)]); // el 1 para que no salte
    }
	
		
	byte[] voicemod = new byte[voice.length*2];
    voicemod = short2byte(samplesmod);
		
	String patheffect = "/storage/sdcard0/MisCuaqs/effect.wav";
	File fileeffect = new File (patheffect);
	try 
	{
		BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(fileeffect));
	    bof.write(voicemod, 0, voicemod.length);
	    bof.close();
	} catch (FileNotFoundException e) {

	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}		     
	
	my_player.PlayCuaq(patheffect);
}


private void myPrueba9() {
	
	String path = this.getFilename();
	File file = new File(path);						
	int size = (int) file.length(); 
	byte[] voice = new byte[size];
	
	  
	
/*	String pathsound =  "/grabador_sonidos_prueba/res/raw/mosca.wav" ;
	 File filesound = new File(pathsound);						
	int sizesound = (int) filesound.length(); 
	byte[] sound = new byte[sizesound];*/
	
	//"C:/Users/Nerea/workspace/grabador_sonidos_prueba/res/raw/mosca.wav"
	//String pathsound =  "/grabador_sonidos_prueba/res/raw/mosca.wav" ;
//	String pathsound =  "C:/Users/Nerea/workspace/grabador_sonidos_prueba/res/raw/mosca.wav" ;
//	File filesound = new File(pathsound);
//	byte[] sound = new byte[(int)filesound.length()];
	//byte[] sound = new byte[(int)file.length()];
	
	//byte[] byteArrayFile = File.ReadAllBytes("C:/Users/Nerea/workspace/grabador_sonidos_prueba/res/raw/mosca.wav");
	
	try {
			BufferedInputStream bif = new BufferedInputStream(new FileInputStream(file));
	        bif.read(voice, 0, voice.length);
	        bif.close();
		// Read the given binary file, and return its contents as a byte array. 
		  
/*			String aInputFileName = "mosca.wav";
		    File fileprueba = new File(aInputFileName);
		    byte[] result = new byte[(int)fileprueba.length()];
		    InputStream input = null;
		    int totalBytesRead = 0;
		    input = new BufferedInputStream(new FileInputStream(file));
		    while(totalBytesRead < result.length)
		    {
		    	int bytesRemaining = result.length - totalBytesRead;
		          //input.read() returns -1, 0, or more :
		          int bytesRead = input.read(result, totalBytesRead, bytesRemaining); 
		          if (bytesRead > 0){
		            totalBytesRead = totalBytesRead + bytesRead;
		          }
		    }		            
	        BufferedInputStream bif = new BufferedInputStream(new FileInputStream(file));
	        bif.read(voice, 0, voice.length);
	        bif.close();
*/	        
	        
	        
	      /*  
	        BufferedInputStream bif2 = new BufferedInputStream(new FileInputStream(filesound));
	        bif2.read(sound, 0, sound.length);
	        bif2.close();
	        */
/*
	        byte[] array = new byte[1024];
	        FileInputStream fis = new FileInputStream(new File("C:/Users/Nerea/workspace/grabador_sonidos_prueba/res/raw/mosca.wav"));
	        FileOutputStream fos = new FileOutputStream(new File("C:/Users/Nerea/workspace/grabador_sonidos_prueba/res/raw/prueba.wav"));
	        int length = 0;
	        while((length = fis.read(array)) != -1)
	        {
	            fos.write(array, 0, length);
	        }
	        fis.close();
	        fos.close();
*/

	        
/*	        
	    	//String pathsound =  "/grabador_sonidos_prueba/res/raw/mosca.wav" ;
	    	//File filesound = new File(pathsound);
	    	//byte[] sound = new byte[(int)filesound.length()];
	        InputStream is = new FileInputStream(filesound);	       
	        is.read(sound, 0, sound.length);
	        is.close();
*/	        
	        
	   /*     SoundPlayer player = new SoundPlayer();
	        player.SoundLocation = @"C:/Users/Nerea/workspace/grabador_sonidos_prueba/res/raw/mosca.wav";
	        player.Play();
	       
	    	       
	        /*InputStream is = new FileInputStream("R.raw.agua");
	        BufferedInputStream bis = new BufferedInputStream(is);
	        DataInputStream dis = new DataInputStream(bis);
	        int effectsize;*/
	        
	     /*	Context context = null;
			InputStream inStream = context.getResources().openRawResource(R.raw.agua);
	        byte[] music = new byte[inStream.available()];
	     */
	        
	        	        
	    } catch (FileNotFoundException e) {
	
	    	e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }	  
	
		
	//Convert from byte to short
	short[] samples = new short[(voice.length / 2)];
	samples = byte2short(voice);
	
//	short [] samplessound = new short[(sound.length / 2)];
				
	// Save modifiedsamples
	// short [] samplesmod = new short[samples.length];
	short [] samplesmod = samples;
	snd.play(fly);
	
	for (int i=0; i<samplesmod.length; i++)
	{

		//samplesmod [i] += samplessound[i];
	}

	//for (int i=0; i<  ; i++)
   /* int buffsize=512;

    // Read the file into the music array.
    int i = 0;

    while (i<buffsize&&dis.available() > 0) {
        musicin[i] = dis.readByte(); 
        i++;
    }

    // Close the input streams.
    dis.close();
    */
    
	
	byte[] voicemod = new byte[voice.length*2];
    voicemod = short2byte(samplesmod);
		
	String patheffect = "/storage/sdcard0/MisCuaqs/effect.wav";
	File fileeffect = new File (patheffect);
	try 
	{
		BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(fileeffect));
	    bof.write(voicemod, 0, voicemod.length);
	    bof.close();
	} catch (FileNotFoundException e) {

	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}		      
	
	//snd.play(fly);
	my_player.PlayCuaq(patheffect);
	
}

private void myPrueba10() 
{
	String path = this.getFilename();
	File file = new File(path);						
	int size = (int) file.length(); 
	byte[] voice = new byte[size];
	
	try {
	        BufferedInputStream bif = new BufferedInputStream(new FileInputStream(file));
	        bif.read(voice, 0, voice.length);
	        bif.close();
	    } catch (FileNotFoundException e) {
	
	    	e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }	         
		
	//Convert from byte to short
	short[] samples = new short[(voice.length / 2)];
	samples = byte2short(voice);
				
	// Save modifiedsamples
	// short [] samplesmod = new short[samples.length];
	short [] samplesmod = samples;
	
	
	/*
	[x, fs]=  auread(“”);
	Buffer = zeros (1,1000);
	Y= zeros (1, length(x));
	F=0.1;
	startIdx =500;
	sineTable = abs (sin(2*pi*f(0:length(x) -1] /fs));
	bufferIndex = round (sineTable * (length(buffer) –startIdx)) + startIdx;

	for i = 1:length(x)
		y(i) = x(i) + buffer (bufferIndex(i));
		buffer (2:end) = buffer (1:end-1)
		buffer (1) = x(i);
	end
	*/
	
	
	
	byte[] voicemod = new byte[voice.length*2];
    voicemod = short2byte(samplesmod);
		
	String patheffect = "/storage/sdcard0/MisCuaqs/effect.wav";
	File fileeffect = new File (patheffect);
	try 
	{
		BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(fileeffect));
	    bof.write(voicemod, 0, voicemod.length);
	    bof.close();
	} catch (FileNotFoundException e) {

	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}		     
	
	my_player.PlayCuaq(patheffect);
	
}


//////////////
/*	
 EJEMPLOS
		

		
//Nueva prueba
		for(int i=0; i<(samplesmod.length)-44-8000; i++)
        { 
        	  	// ESto no tiene en cuenta el lfo
        	//	samplesmod[i+44] = (short) ((float) samples [i+44+7000] + samples [i+44+6000]);        	     	
        	//	samplesmod[i+44] += (short) ((float) samples [i+44+7000] + samples [i+44+6000]);        	     	
        	//	samplesmod[i+44] += (short) ((float) samples [i+44+4000] + samples [i+44+8000]*attenuation);        	     	
        		//sale
        		// He añadido sumas de en lo que si se parece... pero fijo que parecen ecos (metal)... 
        		//--> si en esos se pudiera cambiar el pitch... LINEA 765
        		//_valuePitch 
        
		



 
//LFO --> ruido
			
		double frec= 0.2;
		//double [] lfo= new double [samples.length];
		//short [] lfo= new short [882]; --> longitud de 882 muestras
		short [] lfo= new short [samples.length];
        double amplitude = 0.15 * Short.MAX_VALUE;
        
		for (int i=0; i < lfo.length; i++)
		{
			lfo[i]= (short) ((float)amplitude* Math.sin((2*Math.PI*frec*i)/RECORDER_SAMPLERATE));
			//prueba2[i] = (short)Math.abs((amplitude * Math.sin((2 * Math.PI * i * frec) / RECORDER_SAMPLERATE)));
	  	}
	  	
		
      
      

//Ruidos raros
        for(int i=0; i<(samplesmod.length)-44; i++)
        {       	        	
	       	samplesmod[i+44] += (short)2*samples[i+44]* attenuation;
        }

                
        for(int i=0; i<(samplesmod.length)-44; i++)
        {        	
	        	if (lfo[i] >= 0 && lfo[i] < i)
	            {
	       		samplesmod[i+44] += (short)samples[i-lfo[i]]* attenuation;
	            }
	         //   else
	         //   {
	         //  	 samplesmod[i+44] += (short)samples[i+lfo[i]]* attenuation;
	         //   }
        }               
        
                                  
 */

 // Probando a subir actualizaciones
// Es viernes
}