package com.example.grabador_sonidos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;


import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.util.Log;

import android.content.Context;


public class reproductor
{
private static final int PRIORITY = 1;
	
	private SoundPool _soundPool;
	private int _soundIdAnimacion;
	private Activity _activity;
	//private HashMap<Integer, Integer> soundsMap;
	
	private int _soundID;
	private float _pitchValue=1.0f;
	
	private Boolean loaded = false;

	private Boolean AutoPlay = false;

////////////////////////////////////////////////////////////////////////
	private Context pContext;		// Local copy of app context
	private float masterVolume = 1.0f;	// Master vloume level
	private float leftVolume = 1.0f;	// Volume levels for left and right channels
	private float rightVolume = 1.0f;
	private float balance = 0.5f;		// A balance value used to calculate left/right volume levels
	
	public void PlayCuaq(String mFileName)
	{
		_soundID = _soundPool.load(mFileName, 1);
		AutoPlay=true;
	}
	
	public reproductor(final Activity context)
	{
		_soundPool = new SoundPool(16, AudioManager.STREAM_MUSIC, 0);		  
		 // pContext = context;
		
		
		_activity = context;
		  // Set the hardware buttons to control the music
		context.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // Load the sound
        _soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);        
        _soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId,
                    int status) {
            	Log.i("miApp","Cargo efectos con AutoPlay  = " +AutoPlay.toString());
            	if(AutoPlay)
            	{
            		 loaded = true;
                     
                     // Getting the user sound settings
                     AudioManager audioManager = (AudioManager) context.getSystemService(context.AUDIO_SERVICE);
                  
                     float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                     float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                     float volume = actualVolume / maxVolume;
                     // Is the sound loaded already?
                     if (loaded) {
                     	Log.i("miApp", "Doy pitch");
                     
                         soundPool.play(_soundID, volume, volume, 1, 0, _pitchValue);
                         //soundPool.play(_soundIdAnimacion, volume/4, volume/4, 1, 0, 2.0f);
                         soundPool.play(_soundIdAnimacion, volume, volume, 1, 0, 2.0f);
                         
                         
                         Log.i("Test", "Played sound");
                     }
            	}
               
            }
        });

      	this.addEffect();
	}
	
	
	public void stopPlaying()
	{
		_soundPool.stop(_soundID);
	}

	public void setPitchValue(float pitchValue)
	{
		this._pitchValue = pitchValue;
	}

	
	public void addEffect()
	{	/*	
		   try {
			   AssetFileDescriptor afd = _activity.getAssets().openFd("alarma.wav");
			   MediaPlayer mp = new MediaPlayer();
			   mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
			    
			   _soundIdAnimacion = _soundPool.load(afd.getFileDescriptor(), 0, afd.getLength(), PRIORITY);
			      
			      mp.reset();
			      mp.setDataSource(afd.getFileDescriptor());
			      mp.prepare();
			      mp.start();
			      afd.close();
			    } catch (IOException e) {
			      e.printStackTrace();
			    }
		*/	    
					   
		
		// Original
		/*	   try {
				      AssetFileDescriptor afd = _activity.getAssets().openFd("mosca.wav");
				      _soundIdAnimacion = _soundPool.load(afd.getFileDescriptor(), 0, afd.getLength(), PRIORITY);
				      afd.close();
				    } catch (IOException e) {
				      e.printStackTrace();
				    }
*/
			/*   SoundPool pl = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
		        // 5 indicates the maximum number of simultaneous streams for this SoundPool object

		        int waterSound = pl.load(pContext, R.raw.mosca, 0);
		        // is the audio file I have imported in my project as resource

		       
		                pl.play(waterSound, 1f, 1f, 0, 0, 1);
			   */
/*
			   _soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		       _soundID = _soundPool.load(this, R.raw.alarma, 1);

		        AudioManager audioManager = (AudioManager) _soundPool.getSystemService(AUDIO_SERVICE);
		        float volume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		        _soundPool.play(_soundID, volume, volume, 1, 0, 1f);
*/	
		
/////////////////////////////////////////////		   
/*		   
		   SoundPool quizzlySoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

			soundsMap = new HashMap<Integer, Integer>();
			
	//		soundsMap.put(1, quizzlySoundPool.load(_soundPool.getSystemService(), R.raw.mosca, 1));
						
	//		soundsMap.put(2, quizzlySoundPool.load(this, R.raw.puerta, 1));
			quizzlySoundPool.play(soundsMap.get(1), 1f, 1f, 1, 0, 1f);
*/
	/*		   
			   SoundPool soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
			   AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			           float curVolume = audioManager
			                   .getStreamVolume(AudioManager.STREAM_MUSIC);
			           float maxVolume = audioManager
			                   .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			           float leftVolume = curVolume / maxVolume;
			           float rightVolume = curVolume / maxVolume;
			           int priority = 10;
			           // int priority_1=5;
			           int no_loop = 0;
			           float normal_playback_rate = 1f;


			   final int ID=1;
				soundPool.play(soundPoolMap.get(ID, leftVolume, rightVolume, priority, no_loop, normal_playback_rate);
		*/
		/*	   
			   @Override
			    public void onCreate(Bundle savedInstanceState) {
			        super.onCreate(savedInstanceState);
			        setContentView(R.layout.main);

			        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
			        spool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
			        soundID = spool.load(this, R.raw.error, 1);

			        white = (Button)findViewById(R.id.whiteBtn);
			        white.setOnClickListener(new View.OnClickListener() {
			            public void onClick(View v) {
			                Sound();
			            }
			        });
			    }

			    public void Sound(){
			        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
			        float volume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			        spool.play(soundID, volume, volume, 1, 0, 1f);
			    };
			    */
	}
	 
	
	
	/////////////////////////////////////////////
/*	// Load up a sound and return the id
	public int load(int sound_id)
	{
		return _soundPool.load(_activity, sound_id, 1);
	}
	
	// Play a sound
	public void play(int sound_id)
	{
		_soundPool.play(sound_id, leftVolume, rightVolume, 1, 0, _pitchValue); 	
	}
*/
	
	public void closeSoundPool()
	{
		_soundPool.release();
		_soundPool = null;
	}
}

