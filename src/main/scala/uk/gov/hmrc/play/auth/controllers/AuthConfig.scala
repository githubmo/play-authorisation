/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.auth.controllers

import scala.util.matching.Regex

case class AuthConfig(mode: String = "identity",
                      pattern: Regex = AuthConfig.defaultPatternRegex,
                      anonymousLoginPattern: Regex = AuthConfig.defaultAnonymousPatternRegex,
                      servicePrefix: String = "",
                      account: Option[String] = None,
                      agentRole: Option[String] = None,
                      delegatedAuthRule: Option[String] = None,
                      confidenceLevel: Int) {
  val clValues = Seq(0, 100, 200, 300, 500)
  require(clValues.contains(confidenceLevel), s"Invalid confidence level $confidenceLevel expected one of ${clValues.mkString(",")}")
}

object AuthConfig {
  val defaultPatternRegex = "/([\\w]+)/([^/]+)/?.*".r
  val defaultAnonymousPatternRegex = "/([^/]+)/?.*".r
}

trait AuthParamsControllerConfig {

  import com.typesafe.config.Config
  import net.ceedubs.ficus.Ficus._
  import net.ceedubs.ficus.readers.{StringReader, ValueReader}

  def controllerConfigs: Config

  private lazy val GlobalConfidenceLevel: Option[Int] = controllerConfigs.getAs[Int]("confidenceLevel")

  private implicit val RegexValueReader: ValueReader[Regex] = StringReader.stringValueReader.map(_.r)

  def authConfig(controllerName: String): AuthConfig = {
    implicit val authConfigReader = ValueReader.relative[AuthConfig] { config: Config =>
      AuthConfig(
        mode = config.getAs[String]("mode").getOrElse("identity"),
        pattern = config.getAs[Regex]("pattern").getOrElse(AuthConfig.defaultPatternRegex),
        anonymousLoginPattern = config.getAs[Regex]("anonymous.pattern").getOrElse(AuthConfig.defaultAnonymousPatternRegex),
        servicePrefix = config.getAs[String]("servicePrefix").getOrElse(""),
        account = config.getAs[String]("account"),
        agentRole = config.getAs[String]("agentRole"),
        delegatedAuthRule = config.getAs[String]("delegatedAuthRule"),
        confidenceLevel = config.getAs[Int]("confidenceLevel").orElse(GlobalConfidenceLevel)
          .getOrElse(throw new InvalidConfigurationException(s"confidenceLevel must be set at either global controllers or $controllerName level"))
      )
    }

    controllerConfigs.as[Option[AuthConfig]](s"$controllerName.authParams").getOrElse(AuthConfig(confidenceLevel = GlobalConfidenceLevel
      .getOrElse(throw new InvalidConfigurationException(s"confidenceLevel must be set at either global controllers or $controllerName level"))))
  }
}

class InvalidConfigurationException(msg: String) extends RuntimeException(msg)
