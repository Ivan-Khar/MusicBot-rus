/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
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
package com.jagrosh.jmusicbot.commands.admin;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.AdminCommand;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SettcCmd extends AdminCommand {

  public SettcCmd(Bot bot) {
    super(bot);
    this.name = "settc";
    this.help = "устанавливает текстовый канал для команд";
    this.arguments = "<channel|NONE>";
    this.options = Collections.singletonList(new OptionData(OptionType.CHANNEL, "channel", "Текстовый канал для команд. NONE чтобы очистить.").setRequired(true));
    this.aliases = bot.getConfig().getAliases(this.name);
  }

  @Override
  protected void execute(SlashCommandEvent event) {
    event.deferReply().queue();
    Settings s = event.getClient().getSettingsFor(event.getGuild());
    if (event.getOption("channel").getAsString().equalsIgnoreCase("none")) {
      s.setTextChannel(null);
      event.getHook().editOriginal(event.getClient().getSuccess() + " Теперь команды можно использовать повсюду")
        .queue();
      return;
    }

    String arg = event.getOption("channel").getAsString();
    List<TextChannel> list = FinderUtil.findTextChannels(arg, event.getGuild());
    List<VoiceChannel> listAudio = FinderUtil.findVoiceChannels(arg, event.getGuild());

    if (list.isEmpty() && listAudio.isEmpty())
      event.getHook().editOriginal(event.getClient().getWarning() + " Нет текстового канала с названием \"" + arg + "\"").queue();
    else if (list.size() > 1)
      event.getHook().editOriginal(event.getClient().getWarning() + FormatUtil.listOfTChannels(list, arg)).queue();
    else if (listAudio.size() > 1)
      event.getHook().editOriginal(event.getClient().getWarning() + FormatUtil.listOfVChannels(listAudio, arg)).queue();
    else {
      Channel channel;
      if (!list.isEmpty()) {
        channel = list.get(0);
      } else {
        channel = listAudio.get(0);
      }
      
      s.setTextChannel(channel);
      Logger.getLogger("a").info(s.getTextChannel(event.getGuild()).getId());
      event.getHook().editOriginal(event.getClient().getSuccess() + " Музыкальные каналы теперь можно использовать только в <#" + channel.getId() + ">").queue();
    }
  }

  @Override
  protected void execute(CommandEvent event) {
    if (event.getArgs().isEmpty()) {
      event.reply(event.getClient().getError() + " Напишите нужный канал или 'NONE' для очистки");
      return;
    }

    String arg = event.getArgs();

    Settings s = event.getClient().getSettingsFor(event.getGuild());
    if (arg.equalsIgnoreCase("none")) {
      s.setTextChannel(null);
      event.reply(event.getClient().getSuccess() + " Теперь команды можно использовать повсюду");
    } else {
      List<TextChannel> list = FinderUtil.findTextChannels(event.getArgs(), event.getGuild());
      List<VoiceChannel> listAudio = FinderUtil.findVoiceChannels(arg, event.getGuild());
      Logger.getLogger("a").info(list + " " + listAudio);

      if (list.isEmpty() && listAudio.isEmpty())
        event.reply(event.getClient().getWarning() + " Нет текстового канала с названием \"" + arg + "\"");
      else if (list.size() > 1)
        event.reply(event.getClient().getWarning() + FormatUtil.listOfTChannels(list, arg));
      else if (listAudio.size() > 1)
        event.reply(event.getClient().getWarning() + FormatUtil.listOfVChannels(listAudio, arg));
      else {
        Channel channel;
        if (!list.isEmpty()) {
          channel = list.get(0);
        } else {
          channel = listAudio.get(0);
        }

        s.setTextChannel(channel);
        event.reply(event.getClient().getSuccess() + " Музыкальные каналы теперь можно использовать только в <#" + channel.getId() + ">");
      }
    }
  }
}
