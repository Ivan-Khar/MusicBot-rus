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
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.settings.Settings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class RemoveCmd extends MusicCommand {

  public RemoveCmd(Bot bot) {
    super(bot);
    this.name = "remove";
    this.help = "убирает пластинку из очереди";
    this.arguments = "<позиция|ALL>";
    this.options = Collections.singletonList(new OptionData(OptionType.STRING, "pos", "Позиция пластинки или ALL для всех").setRequired(true));
    this.aliases = bot.getConfig().getAliases(this.name);
    this.beListening = true;
    this.bePlaying = true;
  }

  @Override
  public void doCommand(CommandEvent event) {
    AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
    if (handler.getQueue().isEmpty()) {
      event.replyError("В очереди ничего нет!");
      return;
    }
    if (event.getArgs().equalsIgnoreCase("all")) {
      int count = handler.getQueue().removeAll(event.getAuthor().getIdLong());
      if (count == 0) event.replyWarning("В очереди ничего нет!");
      else event.replySuccess("Успешно очищена очередь.");
      return;
    }
    int pos;
    try {
      pos = Integer.parseInt(event.getArgs());
    } catch (NumberFormatException e) {
      pos = 0;
    }
    if (pos < 1 || pos > handler.getQueue().size()) {
      event.replyError("Позиция в очереди должна быть между 1 и " + handler.getQueue().size() + "!");
      return;
    }
    Settings settings = event.getClient().getSettingsFor(event.getGuild());
    boolean isDJ = event.getMember().hasPermission(Permission.MANAGE_SERVER);
    if (!isDJ) isDJ = event.getMember().getRoles().contains(settings.getRole(event.getGuild()));
    QueuedTrack qt = handler.getQueue().get(pos - 1);
    if (qt.getIdentifier() == event.getAuthor().getIdLong()) {
      handler.getQueue().remove(pos - 1);
      event.replySuccess("Убрана **" + qt.getTrack().getInfo().title + "** из очереди");
    } else if (isDJ) {
      handler.getQueue().remove(pos - 1);
      User u;
      try {
        u = event.getJDA().getUserById(qt.getIdentifier());
      } catch (Exception e) {
        u = null;
      }
      event.replySuccess("Убрана **" + qt.getTrack().getInfo().title + "** из очереди (запрошено " + (u == null ? "кем-то" : "**" + u.getName() + "**") + ")");
    } else {
      event.replyError("Вы не можете убрать **" + qt.getTrack().getInfo().title + "** потому что вы его не добавляли!");
    }
  }

  @Override
  public void doSlashCommand(SlashCommandEvent event) {
    AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
    String args = event.getOption("pos").getAsString();
    if (handler.getQueue().isEmpty()) {
      event.getHook().editOriginal("В очереди ничего нет!")
        .queue();
      return;
    }
    if (args.equalsIgnoreCase("all")) {
      int count = handler.getQueue().removeAll(event.getUser().getIdLong());
      if (count == 0) event.getHook().editOriginal("В очереди ничего нет!")
        .queue();
      event.getHook().editOriginal("Успешно очищена очередь.")
        .queue();
      return;
    }
    int pos;
    try {
      pos = Integer.parseInt(args);
    } catch (NumberFormatException e) {
      pos = 0;
    }
    if (pos < 1 || pos > handler.getQueue().size()) {
      event.getHook().editOriginal("Позиция в очереди должна быть между 1 и " + handler.getQueue().size() + "!")
        .queue();
      return;
    }
    Settings settings = event.getClient().getSettingsFor(event.getGuild());
    boolean isDJ = event.getMember().hasPermission(Permission.MANAGE_SERVER);
    if (!isDJ) isDJ = event.getMember().getRoles().contains(settings.getRole(event.getGuild()));
    QueuedTrack qt = handler.getQueue().get(pos - 1);
    if (qt.getIdentifier() == event.getUser().getIdLong()) {
      handler.getQueue().remove(pos - 1);
      event.getHook().editOriginal("Убрана **" + qt.getTrack().getInfo().title + "** из очереди")
        .queue();
    } else if (isDJ) {
      handler.getQueue().remove(pos - 1);
      User u;
      try {
        u = event.getJDA().getUserById(qt.getIdentifier());
      } catch (Exception e) {
        u = null;
      }
      event.getHook().editOriginal("Убрана **" + qt.getTrack().getInfo().title + "** из очереди (запрошено " + (u == null ? "кем-то" : "**" + u.getName() + "**") + ")")
        .queue();
    } else {
      event.getHook().editOriginal("Вы не можете убрать **" + qt.getTrack().getInfo().title + "** потому что вы его не добавляли!")
        .queue();
    }
  }
}
