package com.example.grabador_sonidos;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundManager {

	private Context pContext;		// Local copy of app context
	private SoundPool sndPool;		// Our SoundPool instance
	private float rate = 1.0f;			// 1.0f // Playback rate
	private float masterVolume = 0.3f;	// 1.0f // Master volume level
	private float leftVolume = 0.3f;	// 1.0f // Volume levels for left and right channels
	private float rightVolume = 0.3f;	// 1.0f 
	private float balance = 0.3f;		// 0.5f // A balance value used to calculate left/right volume levels

	// Constructor, set up the audio manager, store the app context get system volume level.
		public SoundManager(Context appContext)
		{
		  sndPool = new SoundPool(16, AudioManager.STREAM_MUSIC, 0);		  
		  pContext = appContext;
		}
		
		// Load up a sound and return the id
		public int load(int sound_id)
		{
			return sndPool.load(pContext, sound_id, 1);
		}
		// Play a sound
		public void play(int sound_id)
		{
			sndPool.play(sound_id, leftVolume, rightVolume, 1, 0, rate); 	
		}
		

		// Free ALL the things!
		public void unloadAll()
		{
			sndPool.release();		
		}
		
}
