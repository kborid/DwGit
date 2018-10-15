/*----------------------------------------------+
 |												|
 |	ivPlatform.h - InterSound 4 Platform Config |
 |												|
 |		Platform: ADS (ARM)					|
 |												|
 |		Copyright (c) 1999-2007, iFLYTEK Ltd.	|
 |		All rights reserved.					|
 |												|
 +----------------------------------------------*/


/*
 *	TODO: ���������Ŀ��ƽ̨������Ҫ�Ĺ���ͷ�ļ�
 */
//#include <stdio.h>
//#include <crtdbg.h>
/*
 *	TODO: ����Ŀ��ƽ̨�����޸����������ѡ��
 */


#define IV_UNIT_BITS			8			/* �ڴ������Ԫλ�� */
#define IV_BIG_ENDIAN			0			/* �Ƿ��� Big-Endian �ֽ��� */
#define IV_PTR_GRID				4			/* ���ָ�����ֵ */

#define IV_PTR_PREFIX						/* ָ�����ιؼ���(����ȡֵ�� near | far, ����Ϊ��) */
#define IV_CONST				const		/* �����ؼ���(����Ϊ��) */
#define IV_EXTERN				extern		/* �ⲿ�ؼ��� */
#define IV_STATIC				static		/* ��̬�����ؼ���(����Ϊ��) */
#define IV_INLINE				__inline	/* �����ؼ���(����ȡֵ�� inline, ����Ϊ��) */
#define IV_CALL_STANDARD					/* ��ͨ�������ιؼ���(����ȡֵ�� stdcall | fastcall | pascal, ����Ϊ��) */
#define IV_CALL_REENTRANT					/* �ݹ麯�����ιؼ���(����ȡֵ�� stdcall | reentrant, ����Ϊ��) */
#define IV_CALL_VAR_ARG						/* ��κ������ιؼ���(����ȡֵ�� cdecl, ����Ϊ��) */

#define IV_TYPE_INT8			char		/* 8λ�������� */
#define IV_TYPE_INT16			short		/* 16λ�������� */
#define IV_TYPE_INT24			int			/* 24λ�������� */
#define IV_TYPE_INT32			long		/* 32λ�������� */

#define IV_TYPE_ADDRESS			unsigned int		/* ��ַ�������� */
#define IV_TYPE_SIZE			unsigned int		/* ��С�������� */

#define IV_VOLATILE				volatile

#define IV_ANSI_MEMORY			0			/* �Ƿ�ʹ�� ANSI �ڴ������ */
#define	IV_ANSI_STRING			0			/* �Ƿ�ʹ�� ANSI �ַ��������� */

#define IV_ASSERT(exp)			/*_ASSERT(exp)*/ /* ���Բ��� */
#define IV_YIELD				/* ���в���(��Э��ʽ����ϵͳ��Ӧ����Ϊ�����л�����, ����Ϊ��) */

#if defined(DEBUG) || defined(_DEBUG)
	#define IV_DEBUG			1			/* �Ƿ�֧�ֵ��� */
#else
	#define IV_DEBUG			0			/* �Ƿ�֧�ֵ��� */
#endif

/* ����ƽ̨����ѡ������Ƿ��� Unicode ��ʽ���� */
#if defined(UNICODE) || defined(_UNICODE)
	#define IV_UNICODE			1			/* �Ƿ��� Unicode ��ʽ���� */
#else
	#define IV_UNICODE			0			/* �Ƿ��� Unicode ��ʽ���� */
#endif
