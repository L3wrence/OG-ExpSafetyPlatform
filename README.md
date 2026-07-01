# OG-ExpSafetyPlatform

油气工程实验教学与安全评估平台。

本仓库用于小组成员共同开发项目代码。本文档会用尽量简单的方式说明 Git / GitHub 的基础协作方法，帮助初学者理解如何把自己写的代码提交、上传、同步，并最终整合成完整项目。

## 1. 最重要的概念

### 本地仓库

本地仓库就是你电脑上的项目文件夹。

当你使用下面的命令把项目下载到自己电脑后：

```bash
git clone https://github.com/L3wrence/OG-ExpSafetyPlatform-git.git
```

电脑中生成的 `OG-ExpSafetyPlatform-git` 文件夹就是你的本地仓库。

### 远程仓库

远程仓库就是 GitHub 上的项目仓库。

所有成员最终都通过这个 GitHub 仓库共享代码：

```text
https://github.com/L3wrence/OG-ExpSafetyPlatform-git.git
```

### commit：提交到自己电脑

`commit` 的作用是把当前修改保存到自己的本地仓库中。

也就是说，执行 `git commit` 之后，修改只是记录在你自己的电脑里，其他成员和 GitHub 还看不到。

常用命令：

```bash
git add .
git commit -m "说明这次修改了什么"
```

其中：

- `git add .` 表示把当前所有修改加入准备提交区。
- `git commit -m "..."` 表示把这些修改正式提交到本地仓库。
- 引号里的内容要简单说明这次改了什么，例如 `"完成登录页面"`、`"修复实验数据保存问题"`。

### push：上传到 GitHub

`push` 的作用是把你本地已经 commit 的内容上传到 GitHub 远程仓库。

只有执行 `push` 之后，其他成员才能从 GitHub 获取你的代码。

常用命令：

```bash
git push origin main
```

### pull：从 GitHub 下载别人最新的代码

`pull` 的作用是把 GitHub 远程仓库中的最新代码同步到你的本地电脑。

每次开始写代码前，都建议先执行：

```bash
git pull origin main
```

这样可以尽量避免你在旧代码基础上继续开发，减少冲突。

### clone：第一次下载仓库

如果某位成员电脑上还没有这个项目，需要先执行：

```bash
git clone https://github.com/L3wrence/OG-ExpSafetyPlatform-git.git
```

这一步只需要做一次。之后日常同步代码使用 `git pull`，不需要重复 `clone`。

## 2. 新成员第一次加入项目

1. 安装 Git。
2. 找一个想存放项目的本地文件夹。
3. 在该文件夹中打开终端。
4. 执行：

```bash
git clone https://github.com/L3wrence/OG-ExpSafetyPlatform-git.git
```

5. 进入项目文件夹：

```bash
cd OG-ExpSafetyPlatform-git
```

6. 后续使用 IDEA / VS Code 打开这个文件夹即可。

注意：`git clone` 下来的文件夹已经自动被 Git 管理，不要再执行 `git init`。

## 3. 每个人每天推荐的操作流程

### 开始写代码前

先同步 GitHub 上的最新代码：

```bash
git pull origin main
```

### 写代码过程中

可以随时查看当前修改状态：

```bash
git status
```

### 完成一个小功能后

提交到自己的本地仓库：

```bash
git add .
git commit -m "完成某个具体功能"
```

### 想让其他人拿到你的代码时

上传到 GitHub：

```bash
git push origin main
```

### 想获取其他人的最新代码时

从 GitHub 拉取：

```bash
git pull origin main
```

## 4. commit 和 push 的区别

可以把 GitHub 协作理解成下面这个过程：

```text
自己的电脑 commit -> GitHub push -> 队友电脑 pull
```

更具体地说：

| 操作 | 作用 | 其他人能不能看到 |
| --- | --- | --- |
| `git commit` | 保存到自己的本地仓库 | 不能 |
| `git push` | 上传到 GitHub | 能 |
| `git pull` | 从 GitHub 下载最新代码 | 是自己获取别人代码 |
| `git clone` | 第一次下载整个仓库 | 下载完整项目 |

所以：

- 只 `commit` 不 `push`，代码只在自己电脑里。
- `push` 之后，代码才会出现在 GitHub。
- 其他成员需要 `pull`，才能把 GitHub 上的新代码同步到自己电脑。

## 5. 小组高效协作方法

为了让几个人写的代码快速整合，推荐采用下面的方式。

### 先分工，再写代码

不要多人同时改同一个文件的同一部分。开始开发前，先明确每个人负责的模块。

例如：

- 成员 A：登录、注册、用户信息。
- 成员 B：实验数据录入、实验记录管理。
- 成员 C：安全评估算法、评分逻辑。
- 成员 D：页面样式、前端交互、结果展示。

这样可以减少代码冲突。

### 小步提交，不要一次提交巨大修改

推荐完成一个小功能就提交一次。

好的提交信息示例：

```bash
git commit -m "完成用户登录接口"
git commit -m "新增实验记录列表页面"
git commit -m "修复安全评分计算错误"
```

不推荐的提交信息：

```bash
git commit -m "修改"
git commit -m "update"
git commit -m "111"
```

提交信息越清楚，后期越容易知道每个人做了什么。

### 每次开始写代码前先 pull

每天开始写代码，或者每次准备继续开发前，先执行：

```bash
git pull origin main
```

这样可以先拿到队友已经上传的最新代码。

### 每次完成稳定功能后再 push

不要把明显不能运行、没有完成的半成品随意 push 到主分支。

推荐在下面这些时候 push：

- 一个页面基本完成。
- 一个接口基本完成。
- 一个 bug 已经修复。
- 本地运行没有明显报错。

### push 前先确认状态

上传前建议执行：

```bash
git status
```

确认哪些文件被修改了，避免把无关文件提交上去。

如果状态显示 working tree clean，说明当前没有未提交的修改。

## 6. 推荐的整合流程

项目早期如果代码量不大，可以先采用简单流程：

```text
1. 所有人开始前先 pull
2. 各自开发自己负责的模块
3. 完成一个小功能后 commit
4. 本地确认能运行后 push
5. 其他成员 pull 获取最新代码
6. 遇到冲突时，先沟通谁保留哪部分代码，再解决冲突
```

当项目逐渐复杂后，更推荐使用分支开发：

```text
main 分支：保存稳定版本
个人功能分支：每个人开发自己的功能
Pull Request：把个人分支合并到 main
```

例如某成员开发登录功能：

```bash
git pull origin main
git checkout -b feature-login
```

完成后：

```bash
git add .
git commit -m "完成登录功能"
git push origin feature-login
```

然后在 GitHub 上创建 Pull Request，请其他成员检查代码，确认没有问题后再合并到 `main`。

这种方式更适合多人同时开发，可以减少直接改 `main` 分支导致的问题。

## 7. 遇到冲突怎么办

冲突通常发生在多人修改了同一个文件的同一部分。

如果 `git pull` 时提示 conflict，不要慌。一般处理思路是：

1. 打开提示冲突的文件。
2. 找到类似下面的内容：

```text
<<<<<<< HEAD
你本地的代码
=======
GitHub 上别人的代码
>>>>>>> 分支名
```

3. 和相关成员沟通，决定保留哪部分，或者把两部分合并。
4. 删除 `<<<<<<<`、`=======`、`>>>>>>>` 这些标记。
5. 确认代码能运行后重新提交：

```bash
git add .
git commit -m "解决代码冲突"
git push origin main
```

## 8. 团队协作建议

- 每个人每天开始写代码前先 `pull`。
- 每个人完成一个稳定小功能后再 `commit` 和 `push`。
- 不要长时间只在自己电脑上写代码不上传。
- 不要多人同时改同一个核心文件。
- 提交信息要写清楚。
- 发现冲突先沟通，再修改。
- 重要功能合并前，至少让另一个成员检查一下。
- 如果项目进入后期，优先使用功能分支和 Pull Request。

## 9. 最常用命令速查

```bash
# 第一次下载项目
git clone https://github.com/L3wrence/OG-ExpSafetyPlatform-git.git

# 查看当前修改状态
git status

# 获取 GitHub 上的最新代码
git pull origin main

# 添加所有修改
git add .

# 提交到自己的本地仓库
git commit -m "说明这次修改"

# 上传到 GitHub
git push origin main

# 查看当前分支
git branch --show-current
```

记住最核心的一句话：

```text
commit 是保存到自己电脑，push 是上传到 GitHub，pull 是下载队友上传的代码。
```
