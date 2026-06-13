Official repository for MMOItems

### Useful Links

- Purchase the plugin here: https://www.spigotmc.org/resources/mmoitems.39267/
- Development builds: https://phoenixdevt.fr/devbuilds
- Official documentation: https://gitlab.com/phoenix-dvpmt/mmoitems/-/wikis/home
- Discord Support: https://phoenixdevt.fr/discord
- Other plugins: https://www.spigotmc.org/resources/authors/indyuce.253965/

### 简体中文管理端语言包

插件默认生成简体中文管理端语言文件 `plugins/MMOItems/language/admin.yml`，用于汉化游戏内编辑器、常用管理提示和命令参数错误。如果你已经生成过旧的英文 `admin.yml`，请先备份并删除它，再重启服务器或执行 `/mi reload` 让插件重新生成。

`admin.yml` 只翻译管理员看到的 GUI、聊天编辑提示和错误消息；物品 ID、类型 ID、stat ID、命令参数和配置路径会保持内部格式，避免破坏已有配置和脚本。

### Using MMOItems as dependency

Register the PhoenixDevelopment public repository:

```
<repository>
    <id>phoenix</id>
    <url>https://nexus.phoenixdevt.fr/repository/maven-public/</url>
</repository>
```

And then add both `MythicLib-dist` and `MMOItems-API` as dependencies:

```
<dependency>
    <groupId>io.lumine</groupId>
    <artifactId>MythicLib-dist</artifactId>
    <version>1.6.2-SNAPSHOT</version>
    <scope>provided</scope>
    <optional>true</optional>
</dependency>

<dependency>
    <groupId>net.Indyuce</groupId>
    <artifactId>MMOItems-API</artifactId>
    <version>6.9.5-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```
