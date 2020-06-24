/*
 * Copyright (c) 2012-2020 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and
 * limitations there under.
 */
package com.snowplowanalytics.snowplow.enrich.beam
package enrichments

import java.nio.file.Paths

import cats.syntax.option._
import com.spotify.scio.io.PubsubIO
import com.spotify.scio.testing._

class JavascriptEnrichmentSpec extends PipelineSpec {
  "JavascriptEnrichmentSpec" should "enrich using the javascript enrichment" in {
    val appId = "12345678-abcd-1234-abcd-12345678abcd"
    val raw = Seq(
      SpecHelpers.buildCollectorPayload(
        querystring = s"e=ue&aid=$appId".some,
        path = "/i"
      )
    )
    val expected = Map(
      "derived_contexts" -> s"""{"schema":"iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-1","data":[{"schema":"iglu:com.snowplowanalytics.snowplow/web_page/jsonschema/1-0-0","data":{"id":"$appId"}}]}"""
    )

    JobTest[Enrich.type]
      .args(
        "--job-name=j",
        "--raw=in",
        "--enriched=out",
        "--bad=bad",
        "--resolver=" + Paths.get(getClass.getResource("/iglu_resolver.json").toURI()),
        "--enrichments=" + Paths.get(getClass.getResource("/javascript").toURI())
      )
      .input(PubsubIO.readCoder[Array[Byte]]("in"), raw)
      .distCache(DistCacheIO(""), List.empty[Either[String, String]])
      .output(PubsubIO.readString("out")) { o =>
        o should satisfySingleValue { c: String =>
          SpecHelpers.compareEnrichedEvent(expected, c)
        }; ()
      }
      .output(PubsubIO.readString("bad")) { b =>
        b should beEmpty; ()
      }
      .run()
  }
}
