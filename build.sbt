/**
 * 
 * © Copyright 2017 Greg Symons <gsymons@gsconsulting.biz>.
 *
 * Made available under a Creative Commons Attribution-ShareAlike 4.0 
 * International License: http://creativecommons.org/licenses/by-sa/4.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

name := "scalacheck-presentation"

description := "A short presentation about scalacheck"

enablePlugins(MicrositesPlugin)

val scalacheck_presentation = (project in file(".")).
  settings(
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
    )
  )
