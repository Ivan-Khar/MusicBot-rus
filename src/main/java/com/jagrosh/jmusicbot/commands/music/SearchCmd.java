/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SearchCmd extends MusicCommand {

  private final OrderedMenu.Builder builder;
  private final String searchingEmoji;
  protected String searchPrefix = "ytsearch:";

  public SearchCmd(Bot bot) {
    super(bot);
    this.searchingEmoji = bot.getConfig().getSearching();
    this.name = "search";
    this.aliases = bot.getConfig().getAliases(this.name);
    this.arguments = "<название>";
    this.options = Collections.singletonList(new OptionData(OptionType.STRING, "name", "Название пластинки").setRequired(true));
    this.help = "поиск пластинок в YouTube";
    this.beListening = true;
    this.bePlaying = false;
    this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    builder = new OrderedMenu.Builder().allowTextInput(true).useNumbers().useCancelButton(true).setEventWaiter(bot.getWaiter()).setTimeout(1, TimeUnit.MINUTES);
  }

  @Override
  public void doCommand(CommandEvent event) {
    if (event.getArgs().isEmpty()) {
      event.replyError("Напишите аргументы поиска.");
      return;
    }
    event.reply(searchingEmoji + " Поиск... `[" + event.getArgs() + "]`", m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), searchPrefix + event.getArgs(), new ResultHandler(m, event)));
  }

  @Override
  public void doSlashCommand(SlashCommandEvent event) {
    String args = event.getOption("name").getAsString();
    event.getHook().editOriginal(searchingEmoji + " Поиск... `[" + args + "]`").queue();
    bot.getPlayerManager().loadItemOrdered(event.getGuild(), searchPrefix + args, new SlashResultHandler(event.getHook(), event));
  }

  private class ResultHandler implements AudioLoadResultHandler {

    private final Message m;
    private final CommandEvent event;

    private ResultHandler(Message m, CommandEvent event) {
      this.m = m;
      this.event = event;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
      if (bot.getConfig().isTooLong(track)) {
        m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " Эта пластинка (**" + track.getInfo().title + "**)**) длиннее чем разрешенный лимит : `" + FormatUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`")).queue();
        return;
      }
      AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
      int pos = handler.addTrack(new QueuedTrack(track, event.getAuthor())) + 1;
      m.editMessage(FormatUtil.filter(event.getClient().getSuccess() + " Добавлен **" + track.getInfo().title + "** (`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? "" : " в очередь " + pos))).queue();
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
      builder.setColor(event.getSelfMember().getColor()).setText(FormatUtil.filter(event.getClient().getSuccess() + " Результаты поиска: `" + event.getArgs() + "`:")).setChoices().setSelection((msg, i) -> {
        AudioTrack track = playlist.getTracks().get(i - 1);
        if (bot.getConfig().isTooLong(track)) {
          event.replyWarning("Эта пластинка (**" + track.getInfo().title + "**) длиннее чем разрешенный лимит : `" + FormatUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`");
          return;
        }
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        int pos = handler.addTrack(new QueuedTrack(track, event.getAuthor())) + 1;
        event.replySuccess("Добавлена пластинка **" + FormatUtil.filter(track.getInfo().title) + "** (`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? "" : " в очередь " + pos));
      }).setCancel(msg -> {}).setUsers(event.getAuthor());
      for (int i = 0; i < 4 && i < playlist.getTracks().size(); i++) {
        AudioTrack track = playlist.getTracks().get(i);
        builder.addChoices("`[" + FormatUtil.formatTime(track.getDuration()) + "]` [**" + track.getInfo().title + "**](" + track.getInfo().uri + ")");
      }
      builder.build().display(m);
    }

    @Override
    public void noMatches() {
      m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " Результаты не найдены для `" + event.getArgs() + "`.")).queue();
    }

    @Override
    public void loadFailed(FriendlyException throwable) {
      if (throwable.severity == Severity.COMMON)
        m.editMessage(event.getClient().getError() + " Ошибка загрузки: " + throwable.getMessage()).queue();
      else m.editMessage(event.getClient().getError() + " Ошибка загрузки пластинки.").queue();
    }
  }

  private class SlashResultHandler implements AudioLoadResultHandler {

    private final InteractionHook m;
    private final SlashCommandEvent event;

    private SlashResultHandler(InteractionHook m, SlashCommandEvent event) {
      this.m = m;
      this.event = event;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
      if (bot.getConfig().isTooLong(track)) {
        m.editOriginal(FormatUtil.filter(event.getClient().getWarning() +
          " Эта пластинка (**" + track.getInfo().title + "**) длиннее чем разрешенный лимит : `" +
          FormatUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`"))
          .queue();
        return;
      }
      AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
      int pos = handler.addTrack(new QueuedTrack(track, event.getUser())) + 1;
      m.editOriginal(FormatUtil.filter(event.getClient().getSuccess() +
        " Добавлен **" + track.getInfo().title + "** (`" + FormatUtil.formatTime(track.getDuration()) + "`) " +
        (pos == 0 ? "" : " в очередь " + pos)))
        .queue();
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
      builder.setColor(event.getGuild().getSelfMember().getColor()).setText(FormatUtil.filter(event.getClient().getSuccess() +
        " Результаты поиска: `" + event.getOption("name").getAsString() + "`:")).setChoices().setSelection((msg, i) -> {
        AudioTrack track = playlist.getTracks().get(i - 1);
        if (bot.getConfig().isTooLong(track)) {
          m.editOriginal("Эта пластинка (**" + track.getInfo().title + "**) длиннее чем разрешенный лимит : `" +
            FormatUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`")
            .queue();
          return;
        }
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        int pos = handler.addTrack(new QueuedTrack(track, event.getUser())) + 1;
        m.editOriginal("Добавлена пластинка **" + FormatUtil.filter(track.getInfo().title) + "** (`" +
          FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? "" : " в очередь " + pos))
          .queue();
      }).setCancel(msg -> {}).setUsers(event.getUser());
      for (int i = 0; i < 4 && i < playlist.getTracks().size(); i++) {
        AudioTrack track = playlist.getTracks().get(i);
        builder.addChoices("`[" + FormatUtil.formatTime(track.getDuration()) + "]` [**" + track.getInfo().title + "**](" + track.getInfo().uri + ")");
      }
      builder.build().display(event.getChannel());
    }

    @Override
    public void noMatches() {
      m.editOriginal(FormatUtil.filter(event.getClient().getWarning() + " Результаты не найдены для `" +
        event.getOption("name").getAsString() + "`."))
        .queue();
    }

    @Override
    public void loadFailed(FriendlyException throwable) {
      if (throwable.severity == Severity.COMMON)
        m.editOriginal(event.getClient().getError() + " Ошибка загрузки: " + throwable.getMessage())
          .queue();
      else m.editOriginal(event.getClient().getError() + " Ошибка загрузки пластинки.")
        .queue();
    }
  }
}
