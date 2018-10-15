/*----------------------------------------------+
 |												|
 |	com_iflytek_asr_service_Asr.c 	|
 |												|
 |		Copyright (c) 1999-2009, iFLYTEK Ltd.	|
 |		All rights reserved.					|
 |												|
 +----------------------------------------------*/
#include <stdio.h>
#include <malloc.h>
#include <unistd.h>
#include <string.h>
#include <assert.h>
#include <android/log.h>
#include <jni.h>
#include <pthread.h>

#include "Inc/ivESR.h"

#define AITALK_SDK_SN "iflytek#XREEZU5GNMHOOFXCQMZTDBAN5H75Z5I3IVQV2WPUP42TR78O"

#define LOGD(text) __android_log_write(ANDROID_LOG_DEBUG,"AitalkAsr",text)
#define LOGE(text) __android_log_write(ANDROID_LOG_ERROR,"AitalkAsr",text)


// #define  FILE_ASR_LOG "/sdcard/iflyasr/Aitalk3Log.esl"
// #define  DIR_ASR_RESOURCE "/sdcard/iflyasr/"
// #define  DIR_SDCARD "/sdcard/iflyasr/"              //������ʱ�ļ���Ŀ¼
// #define  DIR_SDCARD "/data/data/com.iflytek.asr.AsrService/"              //������ʱ�ļ���Ŀ¼

#define  _DEBUG 	                            //����ʱҪ��
#define  MAX_PATH 256


typedef struct _StJavaAsrClass{
	JNIEnv *env;				
	jclass clazzTts;
	jmethodID onJniMessage;
	jmethodID onJniResult;
}StJavaAsrClass;

static 	TUserOS m_stUserOS = {0};                //�û�ϵͳ�ṹ��
static	ivTResPackDesc m_stResPackDesc = {0};    //��Դ���������ṹ��
static	ivHandle m_hEsrObj = ivNull;             //ʶ��ʵ��
static  FILE *m_pLog = ivNull;                   //��־�ļ�
static StJavaAsrClass m_JavaAsrClass = {0};      //�ص�Java
static char m_strSence[40] = {0};                //��ǰ����

static char m_dirResource[MAX_PATH] = {0};             // ��Դ�ļ�Ŀ¼
static char m_dirGrammar[MAX_PATH] = {0};              // �﷨�ļ�����Ŀ¼
static char m_Eslfile[MAX_PATH] = {0};             // log �ļ�����Ŀ¼



static void JniInit(JNIEnv * env)
{
	jclass classTmp =  env->FindClass("com/iflytek/asr/AsrService/Asr");

	if (NULL == classTmp){
		LOGD("FindClass  is null");
		return;
	}

	m_JavaAsrClass.clazzTts = (jclass)env->NewGlobalRef(classTmp);
	m_JavaAsrClass.env = env;

	LOGD("JniInit  into");
	if (NULL != m_JavaAsrClass.clazzTts ){
		m_JavaAsrClass.onJniMessage  = env->GetStaticMethodID(m_JavaAsrClass.clazzTts
			,"onCallMessage","(I)I");
		m_JavaAsrClass.onJniResult  = env->GetStaticMethodID(m_JavaAsrClass.clazzTts
			,"onCallResult","()I");
		
		LOGD("JniInit  ok");
	}
}

static void myMemCopy( ivPointer pDesc0, ivCPointer pSrc0, ivSize nSize )

{

         ivPByte pDesc;

         ivPCByte pSrc;

         pDesc = (ivPByte)pDesc0;

         pSrc = (ivPCByte)pSrc0;

 

         while ( nSize -- )

                   *pDesc++ = *pSrc++;

}


static void JniDinit()
{
	if (NULL != m_JavaAsrClass.clazzTts){
		m_JavaAsrClass.env->DeleteGlobalRef(m_JavaAsrClass.clazzTts);
		m_JavaAsrClass.clazzTts = NULL;
	}	
	m_JavaAsrClass.env = NULL;
	m_JavaAsrClass.onJniMessage = 0;
	m_JavaAsrClass.onJniResult = 0;	
	LOGD("JniDInit  ok");

}


static void LogComm(const char *comm,const char *memo,int actionId,int ret)
{
#ifdef _DEBUG
	char msg[256] = {0};
	sprintf(msg,"%s %s act=%d ret=%d",comm,memo,actionId,ret);
	LOGD(msg);
#endif 
}

//1. ���ڷ���ص�
static ivPointer ivCall CBRealloc(ivPointer pUser,ivPointer p,ivSize nSize)
{
	return (ivPointer)realloc(p,nSize);
}

//2. �ڴ��ͷŻص�
static void ivCall CBFree(ivPointer pUser,ivPointer p)
{
	free(p);
	p = NULL;
}

//3. ���ļ��ص�����;Ĭ��Ŀ¼��/sdcard/
static ivHandle ivCall CBOpenFile(ivPointer pUser,ivCStr lpFileName,ivInt enMod,ivInt enType)
{
	
	FILE* pf = NULL;
	char szFileName[MAX_PATH] = {0};
	char szMode[8] = {0};

	//���ݲ�ͬ����,����Դ������ͨ�ļ�
	if(ivResFile == enType){
		sprintf(szFileName,"%s%s",m_dirResource,lpFileName);
	}
	else{
		sprintf(szFileName,"%s%s",m_dirGrammar,lpFileName);
	}
	LOGD(szFileName);
	
	if(ivModWrite == enMod){
		pf = fopen(szFileName,"wb");
	}
	else{
		pf = fopen(szFileName,"rb");
	}
	if (NULL == pf){
		LOGD("CBOpenFile open file is null :");
	}
	LOGD("CBOpenFile open file Over!");
	return (ivHandle)pf;
}

//4. �ر��ļ��ص� 
static ivBool ivCall CBCloseFile(ivPointer pUser,ivHandle hFile)
{
	if (NULL == hFile){
		LOGD("CBCloseFile  file is null");
		return 0;
	}
	LOGD("CBCloseFile file Over!");
	return (0 == fclose((FILE*)hFile));
}



//5. ���ļ��ص� 
static ivBool ivCall CBReadFile(ivPointer pUser,ivHandle hFile,ivPByte pBuffer,ivUInt32 iPos,ivSize nSize)
{
	FILE* pf = (FILE*)hFile;
	ivSize nRead = 0;

	if (NULL == pf){
		LOGD("CBReadFile  file is null");
		return 0;
	}
	
	if(FILE_POS_CURRENT != iPos){
		fseek(pf,iPos,SEEK_SET);
	}

	nRead = fread(pBuffer,1,nSize,pf);
	
	return nRead == nSize;
}

//6. д�ļ��ص� 
static ivBool ivCall CBWriteFile(ivPointer pUser,ivHandle hFile,ivPCByte pBuffer,ivUInt32 iPos,ivSize nSize)
{
	FILE* pf = (FILE*)hFile;
	ivSize nWrite = 0;

	if (NULL == pf){
		LOGD("CBWriteFile  file is null");
		return 0;
	}
	
	if(FILE_POS_CURRENT != iPos){
		fseek(pf,iPos,SEEK_SET);
	}
	nWrite = fwrite(pBuffer,1,nSize,pf);
	return nWrite == nSize;
}



//7.1. ��־������Ϣ 
static void OnMsgLog(ivUInt32 wParam, ivCPointer lParam)
{
	int ret = 0;
	if (NULL != m_pLog){
		ret = fwrite(lParam,wParam, 1, m_pLog);
		fflush(m_pLog);
	}
}

// 7.2  ������н���ص�Java
static int OnMsgCallJava(int msgType)
{
	LogComm("OnMsgCallJava","",0,msgType);

	if (m_JavaAsrClass.env && m_JavaAsrClass.clazzTts
		&& m_JavaAsrClass.onJniMessage){
		m_JavaAsrClass.env->CallStaticIntMethod(m_JavaAsrClass.clazzTts
			,m_JavaAsrClass.onJniMessage,(jint)msgType);
	}else{
		LOGD("OnMsgCallJava  but java method null");
	}
	
	return 0;
}

static int m_nResultCount = 0;
static PCEsrResult m_pResult = NULL;

//7.3  ��ʶ������Ϣ����
static void OnMsgResult(ivUInt32 wParam, ivCPointer lParam)
{


	LogComm("OnMsgResult","",  0, wParam);
	PCEsrResult pResult = (PCEsrResult)lParam;
	ivUInt32	nBest = (ivUInt32)wParam;
	
	m_pResult = pResult;
	m_nResultCount = nBest;

	if (m_JavaAsrClass.env && m_JavaAsrClass.clazzTts
		&& m_JavaAsrClass.onJniResult){
	}else{
		LOGD("OnMsgResult  but java method null");
		return ;
	}
	m_JavaAsrClass.env->CallStaticIntMethod(m_JavaAsrClass.clazzTts
		,m_JavaAsrClass.onJniResult,(jint)nBest);

}

static pthread_mutex_t g_tCriticalSection;
static void InitializeCriticalSection(pthread_mutex_t*  pmutex)
{
	int ret = pthread_mutex_init(pmutex,NULL);
}

static void DeleteCriticalSection(pthread_mutex_t*  pmutex)
{
	pthread_mutex_destroy(pmutex);
}

static void EnterCriticalSection(pthread_mutex_t*  pmutex)
{
	pthread_mutex_lock(pmutex);
}

static void LeaveCriticalSection(pthread_mutex_t*  pmutex)
{
	pthread_mutex_unlock(pmutex);
}


//7. ͳһ��Ϣ�ص�
static ivStatus ivCall CBMsgProc(ivPointer pUser,ivHandle hObj,ivUInt32 uMsg,ivUInt32 wParam,ivCPointer lParam)
{
	int  bOK = 0;

	switch(uMsg)
	{
	case ivMsg_ToSleep:
		//ǿ��ִ��sleep
		usleep(wParam);
		break;
	case ivMsg_Create:
		//Create��Ϣ�������ٽ���
		// LOGD("AitalkAsr---InitializeCriticalSection......");
		//InitializeCriticalSection(&g_tCriticalSection);
		break;
	case ivMsg_Destroy:
		// Destroy��Ϣ�������ٽ��� 
		//DeleteCriticalSection(&g_tCriticalSection);
		// LOGD("AitalkAsr---DeleteCriticalSection......");
		break;
	case ivMsg_ToEnterCriticalSection:
		// EnterCriticalSection��Ϣ�������ٽ���
		//EnterCriticalSection(&g_tCriticalSection);
		// LOGD("AitalkAsr---EnterCriticalSection......");
		// return ivErr_FALSE;
		break;
	case ivMsg_ToLeaveCriticalSection:
		// ExitCriticalSection��Ϣ���˳��ٽ��� 
		//LeaveCriticalSection(&g_tCriticalSection);
		// LOGD("AitalkAsr---LeaveCriticalSection......");
		// return ivErr_FALSE;
		break;
	case ivMsg_SpeechStart:
		// SpeechStart��Ϣ����⵽������Ϣ����
		OnMsgCallJava(ivMsg_SpeechStart);
		break;
	case ivMsg_SpeechEnd:
		OnMsgCallJava(ivMsg_SpeechEnd);
		break;
	case ivMsg_ResponseTimeout:
		// ResponseTimeout��Ϣ����Ӧ��ʱ��Ϣ���� 
		OnMsgCallJava(ivMsg_ResponseTimeout);
		break;
	case ivMsg_SpeechTimeout:
		// SpeechTimeOut��Ϣ��������ʱ��Ϣ���� 
		OnMsgCallJava(ivMsg_SpeechTimeout);
		break;
	case ivMsg_ToStartAudioRec:
		//ToStartAudioRec��Ϣ������¼����Ϣ���� 
		OnMsgCallJava(ivMsg_ToStartAudioRec);
		break;
	case ivMsg_ToStopAudioRec:
		//ToStopAudioRec��Ϣ���ر�¼����Ϣ����
		OnMsgCallJava(ivMsg_ToStopAudioRec);
		break;
	case ivMsg_Result:
		//Result��Ϣ��ʶ������Ϣ���� 
		OnMsgResult(wParam,lParam);
		break;	
	case ivMsg_EndedByUser:
		// Result��Ϣ���û������˳�
		// LOGD("Ended By USer Callback!");
		break;	
	case ivMsg_LOG:
		// LOG��Ϣ����־��Ϣ����
		// OnMsgLog(wParam,lParam);
		break;

	case ivMsg_Error:
		LOGD("On Msg Error...");
		LOGD((const char*)lParam);
		break;
	}

	return ivErr_OK;
}



static int ESR_Destroy()
{
	int ret = 0;

	if (NULL != m_hEsrObj){		
		ret = EsrStop(m_hEsrObj);
		EsrDestroy(m_hEsrObj);
		m_hEsrObj = NULL;
		
	}
	if (NULL != m_pLog){
		fclose (m_pLog);
		m_pLog = NULL;
	}
	
	m_nResultCount = 0;
	m_pResult = NULL;
	
	LOGD("ESR_Destroy Ok");
	return ret;
}
static int ESR_Create()
{

	int iStatus = 0;	
	if (NULL != m_hEsrObj){
		// ESR_Destroy();
		return 0;
	}
	memset((ivPByte)&m_stUserOS, 0, sizeof(TUserOS));
	m_stUserOS.nSize = sizeof(TUserOS);
	m_stUserOS.lpszLicence = (ivCharA *)AITALK_SDK_SN;
	m_stUserOS.lpfnRealloc = CBRealloc;
	m_stUserOS.lpfnFree= CBFree;
	m_stUserOS.pPersisRAM = ivNull;
	m_stUserOS.nPersisRAMSize = 0;
	m_stUserOS.lpfnWriteFile = CBWriteFile;
	m_stUserOS.lpfnOpenFile = CBOpenFile;
	m_stUserOS.lpfnCloseFile = CBCloseFile;
	m_stUserOS.lpfnReadFile = CBReadFile;	
	m_stUserOS.lpfnMsgProc = CBMsgProc;
#ifdef _DEBUG
	m_stUserOS.bCheckResource = ivTrue;
	m_pLog = fopen(m_Eslfile, "wb");
#endif
	iStatus = EsrCreate(&m_hEsrObj, &m_stUserOS);
	LogComm("ESR_Create ","",0,iStatus);
	return iStatus;
}

static int utf16len(const ivCharW* str)
{
	int len = 0;
	const ivCharW *p = str;
	while (*p++ != 0){
		len ++;
	}
	return len;
}

jint JNICALL 
JniGetVersion
	(JNIEnv *env, jobject thiz)
{
	jint major = 0;
	int minor = 0;
	EsrGetVersion((ivPUInt8)&major,(ivPUInt8)&minor);	
	return major * 100 + minor;
}

jint JNICALL
JniCreate
	(JNIEnv *env, jobject thiz,jstring dirsource,jstring dirgrammar)
{

	char *pName = NULL;
	if (NULL == dirsource || NULL == dirgrammar){
		return ivErr_Failed;
	}
	// ��ԴĿ¼
	pName = (char *)env->GetStringUTFChars(dirsource,NULL);
	memset(m_dirResource,0,MAX_PATH);
	strcpy(m_dirResource,pName);
	
	env->ReleaseStringUTFChars(dirsource,pName);

	// �﷨�ļ�����Ŀ¼
	pName = (char *)env->GetStringUTFChars(dirgrammar,NULL);
	memset(m_dirGrammar,0,MAX_PATH);
	strcpy(m_dirGrammar,pName);
	
	// log �����ļ�
	memset(m_Eslfile,0,MAX_PATH);
	strcpy(m_Eslfile,pName);
	strcat(m_Eslfile,"Aitalk4Log.esl");
	

	env->ReleaseStringUTFChars(dirgrammar,pName);
	
	LOGD(m_dirResource);
	LOGD(m_dirGrammar);
	LOGD(m_Eslfile);
	// �����﷨
	return ESR_Create();
}

jint JNICALL
JniDestroy
	(JNIEnv *env, jobject thiz)
{
	return  ESR_Destroy();
}

jint JNICALL
JniStart
	(JNIEnv *env, jobject thiz,jstring sceneName)
{
	int ret = ivErr_Failed;
	char *pName = NULL;
	if (NULL == sceneName){
		return ivErr_Failed;
	}
	pName = (char *)env->GetStringUTFChars(sceneName,NULL);
	
	strcpy(m_strSence,pName);
	
	env->ReleaseStringUTFChars(sceneName,pName);

	return ret;
}

jint JNICALL
JniStop
	(JNIEnv *env, jobject thiz)
{
	int ret = ivErr_Failed;
	if (NULL == m_hEsrObj){
		return ivErr_Failed;
	}
	LogComm("EsrStop", "",0, (int)m_hEsrObj);
	ret = EsrStop(m_hEsrObj);
	LogComm("EsrStop", "",0, ret);
	return ret;
}

jint JNICALL
JniExit
	(JNIEnv *env, jobject thiz)
{
	int ret = ivErr_Failed;
	if (NULL == m_hEsrObj){
		return ivErr_Failed;
	}
	LogComm("EsrExitService", "",0, (int)m_hEsrObj);
	ret = EsrExitService(m_hEsrObj);
	LogComm("EsrExitService", "",0, ret);
	return ret;
}


jint JNICALL
JniRunTask
	(JNIEnv *env, jobject thiz)
{
	int iStatus = ivErr_OK;
	
	m_pResult = NULL;
	m_nResultCount = 0;
	
	if (NULL == m_hEsrObj){
		ESR_Create();
	}
	JniInit( env);
	iStatus = EsrStart(m_hEsrObj,(ivChar *)m_strSence);
	LogComm("EsrStart sence  ",m_strSence, 0,iStatus);
	if (ivErr_OK == iStatus){
		iStatus = EsrRunService(m_hEsrObj);
		LogComm("EsrRunService ------------ ret = ","", 0,iStatus);
	}else{
		LogComm("EsrStart sence failed ---------------",m_strSence, 0,iStatus);
	}
	JniDinit();
	return iStatus;
}

//1.1 ȡ�ܽ����
jint JNICALL
JniGetResCount
	(JNIEnv *env, jobject thiz)
{
	return m_nResultCount;
}


//1.2  get sentence id
jint JNICALL
JniGetSentenceId
	(JNIEnv *env, jobject thiz ,jint resIndex)
{

	if (m_pResult == NULL 	|| resIndex < 0 || resIndex >= m_nResultCount){
		return 0;
	}
	return 	m_pResult[resIndex].iSyntaxID;
}

//1.2 get result confidence
jint JNICALL
JniGetConfidence
 (JNIEnv *env, jobject thiz ,jint resIndex)
{

	if (m_pResult == NULL 	|| resIndex < 0 || resIndex >= m_nResultCount){
		return 0;
	}
	return 	m_pResult[resIndex].nConfidenceScore;
}

//1.3 get SlotNumber
jint JNICALL
JniGetSlotNumber
	(JNIEnv *env, jobject thiz ,jint resIndex)
{
	if (m_pResult == NULL 	|| resIndex < 0 || resIndex >= m_nResultCount){
		return 0;
	}
	
	return 	m_pResult[resIndex].nSlot;
}


//1.4 get item Number
jint JNICALL
JniGetItemNumber
	(JNIEnv *env, jobject thiz ,jint resIndex,jint slotIndex)
{
	if (m_pResult == NULL 	|| resIndex < 0 || resIndex >= m_nResultCount){
		return 0;
	}
	if (slotIndex < 0 || slotIndex >= m_pResult[resIndex].nSlot){
		return 0;
	}
	return 	m_pResult[resIndex].pSlots[slotIndex].nItem;
}

//1.5 get item id
jint JNICALL
JniGetItemId
	(JNIEnv *env, jobject thiz ,jint resIndex,jint slotIndex,jint itemIdex)
{
	if (m_pResult == NULL 	|| resIndex < 0 || resIndex >= m_nResultCount){
		return 0;
	}
	if (slotIndex < 0 || slotIndex >= m_pResult[resIndex].nSlot){
		return 0;
	}

	if (itemIdex < 0 || itemIdex >= m_pResult[resIndex].pSlots[slotIndex].nItem){

		return 0;
	}
	return 	m_pResult[resIndex].pSlots[slotIndex].pItems[itemIdex].nID;
}




//1.6 get item text
jstring  JNICALL
JniGetItemText
	(JNIEnv *env, jobject thiz ,jint resIndex,jint slotIndex,jint itemIdex)
{
	if (m_pResult == NULL 	|| resIndex < 0 || resIndex >= m_nResultCount){
		return 0;
	}
	if (slotIndex < 0 || slotIndex >= m_pResult[resIndex].nSlot){
		return 0;
	}

	if (itemIdex < 0 || itemIdex >= m_pResult[resIndex].pSlots[slotIndex].nItem){
		return 0;
	}
	ivCStrW pres = m_pResult[resIndex].pSlots[slotIndex].pItems[itemIdex].pText;
	int len = utf16len(pres);
	if (NULL != pres){
		return  env->NewString(pres,len);
	}
    return NULL;
}

jint  JNICALL
JniAppendData
	(JNIEnv *env, jobject thiz,jbyteArray dataArray,jint dataSize)
{
	if (dataSize <= 0 || m_JavaAsrClass.env == 0){
		LOGD("JniAppendData size is 0");
		return 0;
	}
	jbyte* pBuff = env->GetByteArrayElements(dataArray, NULL);

	if (NULL == pBuff){
		LOGD("JniAppendData dataArray is null");
		return -1;
	}

	int ret = EsrAppendAudioData(m_hEsrObj,(ivPInt16) pBuff,dataSize >> 1);
	LogComm("EsrAppendAudioData","",dataSize,  ret);	

	env->ReleaseByteArrayElements(dataArray, pBuff, 0); 
	return ret;	
	
}


jint JNICALL
JniBuildGrammar
	(JNIEnv* env, jobject thiz, jbyteArray grammarBuff,int buffLen)
{
	jbyte * pBuff = env->GetByteArrayElements(grammarBuff, NULL);

	int iStatus = EsrBuildGrammar(m_hEsrObj, (ivPCByte)pBuff,buffLen);
	
	LogComm("EsrBuildGrammar buffen and ret;" ,"", buffLen ,iStatus);

	env->ReleaseByteArrayElements(grammarBuff,pBuff,0);
	return iStatus;
}


jint JNICALL
JniLexiconCreate
	(JNIEnv *env, jobject thiz,jstring lexiconName)
{
	const ivCStrW pName = (ivCStrW)env->GetStringChars(lexiconName,NULL);
	int iStatus = EsrLexiconCreate(m_hEsrObj,pName);	
	LogComm("EsrBeginLexicon " , "", 0, iStatus);
	
	env->ReleaseStringChars(lexiconName,pName);	
	return iStatus;
}

jint  JNICALL
	JniLexiconUpdate
	(JNIEnv *env, jobject thiz,jstring lexiconName)
{
	LOGD("EsrLexicon Update begin! ");
	const ivCStrW pName = (ivCStrW)env->GetStringChars(lexiconName,NULL);
	int iStatus = EsrLexiconUpdate(m_hEsrObj,pName);
	LogComm("EsrLexicon Update" , "", 0, iStatus);	
	env->ReleaseStringChars(lexiconName,pName);
	return iStatus;
}

jint  JNICALL
	JniLexiconUnload
	(JNIEnv *env, jobject thiz,jstring lexiconName)
{
	LOGD("EsrLexicon Unload begin! ");
	const ivCStrW pName = (ivCStrW)env->GetStringChars(lexiconName,NULL);
	int iStatus = EsrLexiconUnload(m_hEsrObj,pName);
	LogComm("EsrLexicon Unload" , "", 0, iStatus);	
	env->ReleaseStringChars(lexiconName,pName);
	return iStatus;
}


jint JNICALL
JniLexiconInsertItem
	(JNIEnv *env, jobject thiz,jstring lexiconName,jstring word,jint id)
{
  ivCharW mypWord[40]={0};

  int len = env->GetStringLength(word);
	const ivCStrW pName = (ivCStrW)env->GetStringChars(lexiconName,NULL);
	const ivCStrW pWord = (ivCStrW)env->GetStringChars(word,NULL);

	if (NULL == pWord){
		LOGD("EsrAddLexiconItem malloc is null ");
		return -1;
	}
	

  myMemCopy(mypWord,pWord,len*sizeof(ivCharW));

  mypWord[len]=0x00;
	int iStatus = EsrLexiconInsertItem(m_hEsrObj,pName,(ivCStrW)mypWord,id);	
	LogComm("EsrLexiconInsertItem " ,"", 0, iStatus);

	env->ReleaseStringChars(lexiconName,pName);
	env->ReleaseStringChars(word,pWord);
	return  iStatus;	
}

 
jint JNICALL
JniLexiconDeleteItem
	(JNIEnv *env, jobject thiz,jstring lexiconName,jstring word)
{
  ivCharW mypWord[40]={0};

  int len = env->GetStringLength(word);
	const ivCStrW pName = (ivCStrW)env->GetStringChars(lexiconName,NULL);
	const ivCStrW pWord = (ivCStrW)env->GetStringChars(word,NULL);

	if (NULL == pWord){
		LOGD("EsrLexiconDeleteItem malloc is null ");
		return -1;
	}


  myMemCopy(mypWord,pWord,len*sizeof(ivCharW));

  mypWord[len]=0x00;
	int iStatus = EsrLexiconDeleteItem(m_hEsrObj,pName,(ivCStrW)mypWord);
	LogComm("EsrLexiconInsertItem " ,"", 0, iStatus);

	env->ReleaseStringChars(lexiconName,pName);
	env->ReleaseStringChars(word,pWord);
	return  iStatus;
}

jint JNICALL
JniMakeVoiceTag
	(JNIEnv *env, jobject thiz,jstring lexicon,jstring item,jbyteArray buffer,jint length)
{
	/**
	item ʹ��GetStringChars �г�������
	*/
	ivCharW mypItem[40]={0};
	int len = env->GetStringLength(item);
//	char* pLexicon = (char* )env->GetStringUTFChars(lexicon, NULL);
//	char* pItem = (char* )env->GetStringUTFChars(item, NULL);

	const jchar *pLexicon = env->GetStringChars(lexicon, NULL);
	const jchar *pItem = env->GetStringChars(item, NULL);

	//ivStr pLexicon = (ivStr )env->GetStringChars(lexicon, NULL);
	//ivStrW pItem = (ivStrW)env->GetStringChars(item,NULL);

	if (NULL == pItem){
		LOGD("EsrMakeVoicetag malloc is null ");
		return -1;
	}

	myMemCopy(mypItem,pItem,len*sizeof(ivCharW));
	mypItem[len]=0x00;

	jbyte *pBuff = env->GetByteArrayElements(buffer, NULL);

//	LOGD(pLexicon);
//	LOGD(pItem);


	int iStatus =EsrMakeVoicetag(m_hEsrObj,(ivStr)pLexicon,
						 (ivStrW)pItem,
						 (ivPInt16)pBuff,
						 length >> 1);
	LogComm("JniMakeVoiceTag " ,"",length,iStatus);	
//	env->ReleaseStringUTFChars(lexicon,pLexicon);
//	env->ReleaseStringUTFChars(item,pItem);
	env->ReleaseStringChars(lexicon,pLexicon);
	env->ReleaseStringChars(item,pItem);
	env->ReleaseByteArrayElements(buffer,pBuff,0);
	
	return  iStatus;	
}

jint JNICALL
JniSetParam
	(JNIEnv *env, jobject thiz,jint iParam,jint iValue)
{
	
	int iStatus = EsrSetParam(m_hEsrObj,iParam,(ivCPointer)iValue);
	LogComm("JniSetParam ","",iParam,iStatus);	
	return  iStatus;	
}



/*
 * Table of methods associated with a single class.
 */
static JNINativeMethod gMethods[] = {
	//name, signature, funcPtr
	{"JniGetVersion",      "()I", (void*)JniGetVersion},
    {"JniCreate",          "(Ljava/lang/String;Ljava/lang/String;)I", (void*)JniCreate},
    {"JniDestroy",         "()I", (void*)JniDestroy},
    {"JniStart",           "(Ljava/lang/String;)I", (void*)JniStart},
    {"JniStop",            "()I", (void*)JniStop},
    {"JniExit",            "()I", (void*)JniExit},
    
    {"JniRunTask",         "()I", (void*)JniRunTask},
    {"JniGetResCount",     "()I", (void*)JniGetResCount},
    {"JniGetSentenceId",   "(I)I", (void*)JniGetSentenceId},
    {"JniGetConfidence",   "(I)I", (void*)JniGetConfidence},
    {"JniGetSlotNumber",   "(I)I", (void*)JniGetSlotNumber},
    {"JniGetItemNumber",   "(II)I", (void*)JniGetItemNumber},
    
    {"JniGetItemId",       "(III)I", (void*)JniGetItemId},
    {"JniGetItemText",     "(III)Ljava/lang/String;", (void*)JniGetItemText},
    {"JniAppendData",      "([BI)I", (void*)JniAppendData},
    {"JniBuildGrammar",    "([BI)I", (void*)JniBuildGrammar},
    {"JniLexiconCreate",    "(Ljava/lang/String;)I", (void*)JniLexiconCreate},

	{"JniLexiconUpdate",   "(Ljava/lang/String;)I", (void*)JniLexiconUpdate},
	{"JniLexiconUnload",   "(Ljava/lang/String;)I", (void*)JniLexiconUnload},
    {"JniLexiconInsertItem",  "(Ljava/lang/String;Ljava/lang/String;I)I", (void*)JniLexiconInsertItem},
    {"JniLexiconDeleteItem",  "(Ljava/lang/String;Ljava/lang/String;)I", (void*)JniLexiconDeleteItem},
	{"JniMakeVoiceTag",    "(Ljava/lang/String;Ljava/lang/String;[BI)I", (void*)JniMakeVoiceTag},
	{"JniSetParam",        "(II)I", (void*)JniSetParam},
    
};


/*
 * Returns the JNI version on success, -1 on failure.
 */
jint register_com_iflytek_asr_jni(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jclass clazz = NULL; 
    const char* className = "com/iflytek/asr/AsrService/Asr";

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed\n");
        return -1;
    }
    assert(env != NULL);

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class");
        return -1;
    }
    if (env->RegisterNatives(clazz, gMethods,
            sizeof(gMethods) / sizeof(gMethods[0])) < 0) {
        LOGE("RegisterNatives failed for");
        return -1;
    }

    /* success -- return valid version number */
    return JNI_VERSION_1_4;
}


jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    if ( register_com_iflytek_asr_jni(vm, reserved) != JNI_VERSION_1_4) 
		return -1;
    return JNI_VERSION_1_4;
}


