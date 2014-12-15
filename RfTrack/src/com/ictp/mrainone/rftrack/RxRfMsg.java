
// classe per il riconoscimento del messaggio da parte di rf-explorer
//

package com.ictp.mrainone.rftrack;

public class RxRfMsg
{
	//-----------------------------------
	// ring buffer
	private static int rbSize = 1024;					// ring buffer size
	private static ByteRingBuffer ringBuffer;
	//-----------------------------------

	// dimensione massima del messaggio da ricevere
	// - 2 byte iniziali:0x24, 0x53
	// - un byte contenente in numero di valori in Db trasmessi in seguito (normalmente 112, pari a 0x70)
	// - la sequenza di dati (teoricamente 256 valori)
	// - 2 byte finali 0x0D e 0x0A
	public static final int MaxLenMsg = 256 + 3;

	public enum State {

		ST_GET_MSG_START,
		ST_GET_MSG_CHR1,
		ST_GET_MSG_LEN_DATA,
		ST_GET_MSG_DATA,
		ST_GET_MSG_END_CHR0,
		ST_GET_MSG_END_CHR1;

		int constant() {
			return this.ordinal();
		}
	}
	State state;			// stato della macchina di ricezione
	
	public int lenMsg;
	public int lenData;		// lunghezza della parte dati messaggio
	public boolean RxAll;
	public byte[] msgRx = new byte[MaxLenMsg];

	// pulisci il buffer di messaggio
	public void clean()
	{
		RxAll = false;
		initStateRxMsg();			// inizializza la macchina a stati per la lettura messaggio
	}

	// costruttore
	public RxRfMsg()
	{
		ringBuffer = new ByteRingBuffer(rbSize);			// crea il ringbuffer
		this.clean();
	}

	// forma il messaggio per la lettura dei dati
	// #<Size>C2-F:<Start_Freq>, <End_Freq>, <Amp_Top>, <Amp_Bottom>
	// <Size> =	Binary byte, Total size of the message in bytes. Size is limited to max 64 bytes.
	// <Start_Freq> = 7 ASCII digits, decimal KHZ, value of frequency span start (lower)
	// <End_Freq> =	7 ASCII digits, decimal	KHZ, value of frequency span end (higher)
	// <Amp_Top> = 4 ASCII digits, decimal dBm,	Highest value of amplitude for GUI 
	// <Amp_Bottom> = 4 ASCII digits, decimal dBm, Lowest value of amplitude for GUI 
	//
	public byte[] MsgConfigurationData(
        int Start_Freq,		// KHZ, Value of frequency span start (lower)
        int End_Freq,		// KHZ, Value of frequency span end (higher)
        int Amp_Top,		// dBm, Highest value of amplitude for GUI
        int Amp_Bottom)		// dBm, Lowest value of amplitude for GUI
	{
		String msg = String.format("# C2-F:%07d,%07d,-%03d,-%03d", 
						Start_Freq, 
						End_Freq, 
						Math.abs(Amp_Top), 
						Math.abs(Amp_Bottom));
		return(msg.getBytes());
	}
	
	// inizializza la macchina a stati per la lettura messaggio
	private void initStateRxMsg()
	{
		lenMsg = 0;
		state = State.ST_GET_MSG_START;
	}
	
	// controlla se un byte e' presente nel buffer circolare.
	// Esegue la ricerca fino a una posizione pari a len
	private boolean FindByteMsg(Byte val, int pos, int len)
	{
		int i, rd;
		
		// continua a leggere un solo carattere fino a che non hai trovato in carattere inizialw
		for(i=0; i<len; i++)
		{
			rd = ringBuffer.read(msgRx, pos, 1);
			if(rd == 0)
			{
				// il buffer circolare e' vuoto oppure len e' maggiore della sua lunghezza
				return(false);
			}
			if(msgRx[pos]==val)
			{
				// byte trovato
				return(true);
			}
		}
		return(false);
	}
	
	// leggi la parte dati del messaggio
	private boolean GetMsgData()
	{
		int n_byte_rd;						// numero di byte letti
		int len = lenData - (lenMsg - 3);	// n. byte di dati ancora da ricevere

		// leggi il buffer
		n_byte_rd = ringBuffer.read(msgRx, lenMsg, len);
		lenMsg = lenMsg + n_byte_rd;		// aggiorna la lunghezza del messaggio
		if(n_byte_rd < len)
			return(false);
		return(true);
	}
	
	public boolean updateRx(byte[] data)
	{
		//------------------------------------
		// aggiorna il ring buffer con i dati appena ricevuti
		ringBuffer.write(data, 0, data.length);
		//------------------------------------

		while(ringBuffer.getUsed()>0)
		{
			if(RxAll == true)
				break;
		
			switch (state)
			{
			case ST_GET_MSG_START:
				if(FindByteMsg((byte)'$', 0, ringBuffer.getUsed()))
				{
					lenMsg = 1;
					state = State.ST_GET_MSG_CHR1;
				}
				// carattere non trovato
                break;
			case ST_GET_MSG_CHR1:
				if(FindByteMsg((byte)0x53, 1, 1))
				{
					lenMsg = 2;
					state = State.ST_GET_MSG_LEN_DATA;
				}
				else
				{
					// carattere non trovato
					initStateRxMsg(); 		// reinizializza la macchina a stati per  il riconoscimento
				}
				break;
			case ST_GET_MSG_LEN_DATA:
				// leggi il terzo carattere, lunghezza della parte dati del messaggio
				ringBuffer.read(msgRx, 2, 1);
				lenData = (int)(msgRx[2]);
				lenMsg = 3;
				state = State.ST_GET_MSG_DATA;
				break;
			case ST_GET_MSG_DATA:
				if(GetMsgData())
				{
					state = State.ST_GET_MSG_END_CHR0;
				}
                break;
			case ST_GET_MSG_END_CHR0:
				if(FindByteMsg((byte)0x0D, lenMsg, 1))
				{
					lenMsg++;
					state = State.ST_GET_MSG_END_CHR1;
				}
				else
				{
					// carattere non trovato
					initStateRxMsg(); 		// reinizializza la macchina a stati per il riconoscimento
				}
				break;
			case ST_GET_MSG_END_CHR1:
				if(FindByteMsg((byte)0x0A, lenMsg, 1))
				{
					// messaggio ricevugto completamente
					lenMsg++;
					RxAll = true;
				}
				else
				{
					// carattere non trovato
					initStateRxMsg(); 		// reinizializza la macchina a stati per il riconoscimento
				}
				break;
			}
		}
		//------------------------------------
		return(RxAll);
	}
	
	// restituisce true se ha ricevuto tutto il messaggio
	public boolean ChkRx()
	{
		return(RxAll);
	}

	public byte[]GetMsg()
	{
		byte[] result = new byte[lenMsg];
		
		// arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
		System.arraycopy(msgRx, 0, result, 0, lenMsg);
		return(result);
	}
}			// end class RxRfMsg

// end public class RxRfMsg
