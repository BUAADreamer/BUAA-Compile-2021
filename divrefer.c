#include<iostream>
#include<stdio.h>
#include<string.h>
#include<algorithm>
#define maxn 500005
using namespace std;
typedef long long ll;
int N = 32;
int myabs(int now)
{
	return now > 0 ? now : -now;
}
int mylog(int now)
{
	int cnt = 0;
	while(now!=0)
	{
		cnt+=1;
		now>>=1;
	}
	return cnt;
}
int xsign(int now)
{
	return now>=0 ? 0 : -1 ;
}
ll mypow[maxn];
int mulsh(int a,int b)
{
	// 注意这里在mips里是取hi
	ll res = 1ll * a * b;
	return res >> 32;
}
int sra(int a,int b)
{
	return a>>b;
}
int eor(int a,int b)
{
	return a^b;
 }
int mydiv(int n,int d)
{
	int l = max(1,mylog(myabs(d)));
	ll m = 1ll + mypow[N+l-1]/myabs(d);

	int m_ = m-mypow[N];
	int d_sign = xsign(d);
	int sh_post = l - 1;
	// 以上只和d有关和n无关，是在编译的时候可以预处理的

	// 以下和n有关，需要转换为编译出的指令
	int q_0 = n + mulsh(m_,n);
	q_0 = sra(q_0,sh_post)-xsign(n);
	int q = eor(q_0,d_sign)-d_sign;
	return q;
}
int main()
{
	mypow[0]=1;
	for(int i=1;i<=100;i++)
	{
		mypow[i]=mypow[i-1]*2;
	}

	for(int i=1;i<=10000;i++)
	{
		int f1 = rand()%2;
		f1 = f1==0 ? 1 : -1;

		int f2 = rand()%2;
		f2 = f2==0 ? 1 : -1;

		int a=rand(),b=rand();
		if(b==0)
			b=rand();
		if(mydiv(f1*a,f2*b)!=(f1*a)/(f2*b))
		{
			cout<<"False"<<endl;
			cout<<a<<" "<<b<<endl;
			return 0;
		}
	}
	cout<<"True"<<endl;
}

