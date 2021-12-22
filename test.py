import os
import shutil
import time

# 辅助用例库配置:testtype=0,自定义 testtype=1
testtype = 0
# config your own compiler jar filesrc here
compiler_jar_src = "./out/artifacts/Compiler_jar/Compiler.jar"

# testfile directory list
testfile_src_ls = ['test/A', 'test/B', 'test/C']
# testfile_src_ls = ['test/A']

# testfile num in directory and your testfile name should be like "testfile1.txt"
numls = [26, 27, 29]
# numls = [26]

# your mips.txt and output.txt generate src correspond to testfile directory
myoutsrc_ls = ['myoutput/A', 'myoutput/B', 'myoutput/C']


# myoutsrc_ls = ['myoutput/A']


def getCurTime():
    return time.strftime("%Y-%m-%d-%H-%M", time.localtime())


def test():
    wafile_ls = []
    wanum = 0
    for i in range(len(testfile_src_ls)):
        print("%s test begin" % testfile_src_ls[i])
        ret = 0
        for j in range(1, numls[i] + 1):
            ret = testonefile(testfile_src_ls[i], j, myoutsrc_ls[i])
            if ret != 0:
                wafile_ls.append(testfile_src_ls[i] + "/testfile%d.txt" % j)
            wanum += ret
        if ret == 0:
            print("\r%s test end" % testfile_src_ls[i], end='\n')
        else:
            print("%s test end" % testfile_src_ls[i])
    if wanum > 0:
        print('\nyou wa %d points' % wanum)
        print('your wa file list:')
        print(wafile_ls)
        return wafile_ls
    else:
        print('Congratulations!You accept all points')
        return wafile_ls


def checkscore(scorefilename=None):
    if scorefilename == None:
        scorefilename = 'score/' + getCurTime() + '.txt'
    #         print(scorefilename)
    sum = 0
    cnt = 0
    if scorefilename != None and scorefilename != '':
        with open(scorefilename, "w", encoding="utf-8") as f:
            f.write(input("请输入此次代码的描述:") + "\n")
            for i in range(len(myoutsrc_ls)):
                for j in range(1, numls[i] + 1):
                    cnt += 1
                    scorefile = myoutsrc_ls[i] + "/InstructionStatistics%d.txt" % j
                    scoreline = open(scorefile, "r").readlines()[-1]
                    score = float(scoreline.split(":")[1].strip())
                    print(testfile_src_ls[i] + "/testfile%d.txt" % j, scoreline)
                    f.write(testfile_src_ls[i] + "/testfile%d.txt" % j + str(scoreline) + "\n")
                    sum += score
            print("average cycle:%d" % (sum / cnt))
            f.write("average cycle:%d" % (sum / cnt) + "\n")
        return
    # ''时视为此次为随便测测，不输出到文件
    for i in range(len(myoutsrc_ls)):
        # noinspection PyInterpreter
        for j in range(1, numls[i] + 1):
            cnt += 1
            scorefile = myoutsrc_ls[i] + "/InstructionStatistics%d.txt" % j
            scoreline = open(scorefile, "r").readlines()[-1]
            score = float(scoreline.split(":")[1].strip())
            print(testfile_src_ls[i] + "/testfile%d.txt" % j, scoreline)
            sum += score
    print("average cycle:", sum / cnt)


def testonefile(testfile_src, num, myoutsrc):
    shutil.copy(testfile_src + '/testfile%d.txt' % num, 'testfile.txt')
    shutil.copy(testfile_src + '/input%d.txt' % num, 'input.txt')
    shutil.copy(testfile_src + '/output%d.txt' % num, 'ans.txt')
    os.system("java -jar Compiler.jar")
    shutil.copy('mips.txt', myoutsrc + "/mips%d.txt" % num)
    os.system("java -jar Mars-Compile-2021.jar mips.txt < input.txt > my.txt ")
    shutil.copy('my.txt', myoutsrc + "/output%d.txt" % num)
    shutil.copy('InstructionStatistics.txt', myoutsrc + "/InstructionStatistics%d.txt" % num)
    my = open("my.txt", "r").readlines()[2:]
    ans = open("ans.txt", "r").readlines()
    if len(ans) == len(my):
        for i in range(len(ans)):
            if ans[i].rstrip() != my[i].rstrip():
                print("\nwrong on line %d at %s-%d point" % (i + 1, testfile_src, num))
                print("we expect %s while we got %s" % (ans[i], my[i]))
                return 1
        print("\rAccept on point %d" % num, end='', flush=True)
        return 0
    else:
        print("\nWrong at %s-%d point" % (testfile_src, num))
        print("we expect %d line while we got %d line" % (len(ans), len(my)))
        return 1


def init():
    if testtype == 1:
        # 自定义测试库配置:
        # testfile directory list
        global testfile_src_ls
        testfile_src_ls = ['test/D']
        # testfile num in directory and your testfile name should be like "testfile1.txt"
        global numls
        numls = [2]
        # your mips.txt and output.txt generate src correspond to testfile directory
        global myoutsrc_ls
        myoutsrc_ls = ['myoutput/D']
    if not os.path.exists('myoutput'):
        os.mkdir('myoutput')
    if not os.path.exists('test'):
        os.mkdir('test')
    if not os.path.exists('score'):
        os.mkdir('score')
    for src in testfile_src_ls:
        if not os.path.exists(src):
            os.mkdir(src)
    for src in myoutsrc_ls:
        if not os.path.exists(src):
            os.mkdir(src)


def copy(num1, num2, src1, src2):
    shutil.copy(src1 + 'testfile%d.txt' % num1, src2 + 'testfile%d.txt' % num2)
    shutil.copy(src1 + 'input%d.txt' % num1, src2 + 'input%d.txt' % num2)
    shutil.copy(src1 + 'output%d.txt' % num1, src2 + 'output%d.txt' % num2)


if __name__ == '__main__':
    copy(2, 2, 'test/B/', 'test/D/')
    testtype = 0
    testsample = True
    if testsample:
        init()
        shutil.copy(compiler_jar_src, './Compiler.jar')
        ret = test()
        if len(ret) == 0:
            checkscore('')

#     os.system("java -jar Mars-Compile-2021.jar mips.txt < input.txt > my.txt ")
