package com.example.grabador_sonidos;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
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
		
		for(int i=0; i<(samplesmod.length)-44-16000; i++) //4000 //8000
		{ 
	      	samplesmod[i+44] += (short) ((float) samples [i+44+10000]*0.7 +  samples [i+44+16000]*0.4);        	     	
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


}