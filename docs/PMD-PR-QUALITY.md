# PMD quality checks for Pull Requests

ตรวจเฉพาะไฟล์ production Java ที่ PR เพิ่ม แก้ไข หรือเปลี่ยนชื่อ ใน `backend/src/main/java/**/*.java` เมื่อ PR มีปลายทางเป็น `develop` หรือ `main` ใช้แนวทางตรวจทั้งไฟล์ที่เปลี่ยน (แนวทางที่ 1) และเก็บหลักฐานแยกตาม PR/run/attempt

## ขอบเขตและสถานะ

- Check ที่ต้องเลือกใน branch protection คือ **`pmd-scan`** จาก workflow **PMD Code Quality Check**
- Checkout PR head SHA โดยตรง แล้วเปรียบเทียบ `merge-base(base SHA, head SHA)..head SHA` ซึ่งเป็นขอบเขตแบบ three-dot ไม่รวมไฟล์ที่เปลี่ยนเฉพาะฝั่ง branch ปลายทาง
- ตรวจ PMD ทั้งไฟล์ที่เลือกและเก็บผลดิบทุกบรรทัด แต่ check ตัดสินจากทุก violation ที่ `overlaps_changed_lines=true` ตามเกณฑ์ที่ผู้ใช้อนุมัติ ทั้ง `own`, `imported`, `unknown` และทั้ง confidence `high`/`low` มีผลต่อ check เท่ากัน
- ไม่ตรวจ Java ที่ไม่เปลี่ยน, `src/test/java`, frontend หรือไฟล์ที่ลบแล้ว ไฟล์ที่เปลี่ยนชื่อยังถูกตรวจถ้าเส้นทางใหม่อยู่ในขอบเขต
- ทุก PR ในสอง branch นี้มี check แม้ไม่มี Java ที่เปลี่ยน เพื่อให้ required check ไม่ค้าง Pending จาก path filter สถานะกรณีนี้คือ `no_java_changes` และไม่ดาวน์โหลด/รัน PMD
- `passed`: ไม่มี violation ตรงกับช่วงบรรทัดเปลี่ยนและไม่มีข้อผิดพลาดของ PMD/รายงาน; `failed`: พบ violation ตรงช่วงบรรทัดเปลี่ยน; `error`: เครื่องมือ/การเตรียมข้อมูล/รายงานมีข้อผิดพลาด ทั้ง `failed` และ `error` ทำให้ job แดง Attribution ที่ระบุไม่ได้ไม่ทำให้ PMD error และไม่ยกเว้น findings จาก check
- ใช้ PMD **7.10.0**, Java **21**, ทุก priority **1–5**, ทุก rule ใน `bestpractices`, `codestyle`, `errorprone` ไม่มี rule exclusion ที่เพิ่มโดย workflow นี้ ไม่ปิด fail-on-violation หรือ fail-on-error
- เก็บ suppressed findings ที่มีอยู่ใน source ตามที่ PMD รายงานด้วย `--show-suppressed` แยกจาก violations ไม่มีการเพิ่ม suppression เพื่อทำให้ผ่าน
- รัน PMD สองครั้งด้วย input/config เดียวกันเพื่อเก็บ native JSON และ XML แบบครบถ้วน ไม่ compile Maven จึงไม่ต้องมี DB หรือ secret; ไม่มี auxiliary classpath ของ dependency บางกฎที่อาศัย type resolution จึงอาจให้ผลไม่ครบเท่าการตรวจหลัง compile
- PMD CLI ยังใช้ `--fail-on-violation --fail-on-error` และเก็บ native exit code เดิม: code **4** อาจมาจากบรรทัดเดิม จึงให้ `finalize` ตัดสินตามช่วง diff; code ที่เป็นเครื่องมือ/parse/config error ยังทำให้ล้มเหลว ไม่มี `continue-on-error` หรือการตัดรายงานดิบทิ้ง

## ที่เก็บ

ไฟล์ตั้งค่าที่ commit:

| ตำแหน่ง | หน้าที่ |
| --- | --- |
| `.github/workflows/pmd-diff.yml` | Trigger, job status, ติดตั้ง PMD, อัปโหลดหลักฐาน |
| `config/pmd/ruleset.xml` | กฎตรวจส่วนกลาง |
| `scripts/pmd_diff.py` | เลือกไฟล์จาก git diff, เก็บ context, สรุปผล |
| `scripts/pmd-scan.sh` | รัน CLI และเก็บ native reports/logs/exit codes |
| `scripts/pmd_aggregate.py` | รวม attribution จากหลาย run โดยเลือกผลล่าสุดต่อ repository/PR |
| `scripts/tests/test_pmd_diff.py` | ทดสอบขอบเขต diff และการไม่แสดง pass เมื่อรายงานมีปัญหา |
| `scripts/tests/test_pmd_aggregate.py` | ทดสอบการเลือก run/attempt ล่าสุดและสรุปข้าม PR |

ผลตรวจชั่วคราวอยู่ที่ `backend/target/pmd-diff/` ซึ่งถูก ignore โดย `backend/.gitignore` เดิม ผลตรวจไม่ถูก commit รวมกับ source

บน GitHub เก็บเป็น Actions Artifact ชื่อ:

```text
pmd-pr-<PR number>-run-<run ID>-attempt-<attempt number>
```

เปิด PR → Checks → `pmd-scan` → Details → ดู Summary และลิงก์ดาวน์โหลด หรือเปิด Actions → workflow run → Artifacts แต่ละ run/attempt มี artifact ของตัวเอง ไม่มีการเขียนทับรายงานของคนอื่น ใช้ concurrency `pmd-<PR number>` และ `cancel-in-progress: true` เมื่อ push ซ้ำ run เก่าที่ยังทำงานจะถูกยกเลิก Run ที่ยกเลิกอาจมีหลักฐานไม่ครบหรือยังไม่ได้อัปโหลด ต้องใช้ผล run ล่าสุดที่เสร็จจริง ไม่ถือว่า run ที่ถูกยกเลิกผ่าน

กำหนด retention **90 วัน** ซึ่งต้องอยู่ภายในนโยบาย retention ของ repository/organization Artifact เป็นหลักฐานชั่วคราว ไม่ใช่ที่เก็บถาวร หากต้องเก็บเกินกำหนด ให้ดาวน์โหลด archive ก่อนหมดอายุไปยังคลังของทีมที่มีสิทธิ์เข้าถึงเทียบเท่า repository ขณะนี้ยังไม่ได้เชื่อมคลังภายนอก

## ข้อมูลที่เก็บ

| ไฟล์ | ข้อมูล |
| --- | --- |
| `metadata.json` | Schema version, PR number/URL/title/author/draft/labels/timestamps, branches/repos, base/head/merge-base SHA, run/attempt/actor/workflow/job/runner/run URL, tool/config version, scope, retention |
| `changed-files.json` | ทุกไฟล์ที่ PR เปลี่ยนและ Git status; Java มีจำนวนบรรทัดเพิ่ม–ลบ; ไฟล์ส่งตรวจมี SHA-256, Git blob SHA และช่วงบรรทัดเปลี่ยน |
| `commits.json` | Commit SHA, author/committer name, authored/committed timestamps และ subject ของ commit ในช่วง diff ไม่เก็บ email |
| `files.txt` | รายชื่อไฟล์ production Java ที่ส่ง PMD จริง หนึ่งไฟล์ต่อบรรทัด |
| `java.diff.patch` | Diff เฉพาะการเปลี่ยนแปลง Java ในขอบเขต รวม rename/deletion |
| `ruleset.xml` | สำเนากฎที่ใช้จริงใน run นั้น |
| `pmd.json`, `pmd.xml` | Native PMD reports: finding locations, rules, priorities, messages, context, processing/configuration errors และข้อมูลอื่นที่ PMD รองรับ |
| `violations.json` | ทุก finding แบบ normalized พร้อม `overlaps_changed_lines` โดยไม่ตัดจำนวน |
| `attribution.json` | ทุก finding พร้อม Git author/commit, `own`/`imported`/`unknown`/`not_in_scope`, confidence/reason, contributors และช่วงบรรทัดที่ blame รองรับ, context PR/run/check, warnings และ summary รายคน/ประเภท/กฎ |
| `violations.csv` | ตารางสำหรับ Excel/Sheets: file, line/column, rule/ruleset/priority/description, package/class/method/variable, rule URL และ diff overlap; ป้องกันข้อความใน source ถูกเปิดเป็น spreadsheet formula |
| `errors.json`, `suppressed.json` | ข้อผิดพลาดและ suppression ที่ PMD รายงาน |
| `status.json` | ผ่าน/ไม่ผ่าน/ไม่มี Java/error, exit codes, จำนวนไฟล์, เวลาเริ่ม–จบ UTC/ระยะเวลา, counts แยก rule/priority/file/diff overlap |
| `summary.md` | สรุปสถานะและสถิติ พร้อมตารางรายคนแยก own/imported/unknown, high/low และกฎ; ตาราง finding แสดงสูงสุด 100 รายการ แต่รายงานอื่นเก็บครบ |
| `pmd-version.txt`, `java-version.txt` | เวอร์ชันเครื่องมือที่รันจริง |
| `command-json.txt`, `command-xml.txt` | คำสั่งและ flags ที่ใช้จริง |
| `pmd-json.log`, `pmd-xml.log` | stdout/stderr ของ PMD และ benchmark |
| `prepare.log`, `install.log` | Log การเตรียม diff และดาวน์โหลด/ตรวจ checksum |
| `exit-json.txt`, `exit-xml.txt` | Native exit code ของ PMD แต่ละรูปแบบ |
| `manifest.json` | SHA-256 ของทุกไฟล์หลักฐานเพื่อเช็กความถูกต้องหลังดาวน์โหลด |

ไม่มี Java ที่เปลี่ยนจะไม่มี native PMD reports/version/command/logs เพราะไม่ได้เรียกเครื่องมือ หาก checkout/setup/เครื่องมือเสียหรือ runner ถูกยกเลิกก่อนอัปโหลด อาจได้หลักฐานบางส่วนหรือไม่มี artifact ให้ตรวจ log ของ Actions ด้วย ไม่ถือว่ากรณีเหล่านี้ผ่าน

เก็บเฉพาะ context ที่ระบุ ไม่อัปโหลด raw GitHub event, environment ทั้งหมด, tokens, secrets, `.env` หรือ source ทั้ง repository ตัว diff/ข้อความ finding อาจมีโค้ดของทีม ต้องใช้สิทธิ์เข้าถึงตาม repository

## Attribution: ใครเขียน และมั่นใจแค่ไหน

`finalize` ใช้ `git blame --line-porcelain <head SHA>` จากประวัติเต็ม และใช้เฉพาะส่วนของช่วง finding ที่ทับบรรทัดเปลี่ยนใน three-dot diff ไม่เอาบรรทัดเดิมใน finding ที่ยาวหลายบรรทัดมาระบุเจ้าของร่วม

กำหนด commit ที่มีสิทธิ์เป็นงานของ diff ด้วย `git rev-list head --not base` ซึ่งตัด **ทุก commit ที่เข้าถึงได้จาก base** ออก จากนั้นแบ่งตาม topology:

| ประเภท | ความหมาย |
| --- | --- |
| `own` | Commit อยู่บน first-parent ของ PR head และไม่อยู่ใน base เป็นงานบนแนวประวัติหลักของ PR นี้ ไม่ได้หมายความว่า Git author ต้องเป็นคนเดียวกับผู้เปิด PR |
| `imported` | Commit ไม่อยู่ใน base และเข้ามาผ่านประวัติด้านข้างของ merge เก็บชื่อ Git author เดิม ไม่โยนชื่อให้ผู้เปิด PR |
| `unknown` | Git/blame/commit ไม่ครบ, commit ไม่อยู่ในขอบเขต head-minus-base, finding ครอบคลุมหลาย author/หลายประเภท หรือระบุ provenance ไม่ได้ |
| `not_in_scope` | Finding ไม่ทับบรรทัดที่ PR เปลี่ยน เก็บเป็นข้อมูลอย่างเดียว ไม่มีผลต่อ check และไม่มีการระบุเจ้าของ |

ข้อมูลแต่ละ finding ประกอบด้วย `violation_index` (ตำแหน่งใน `violations.json`), `violation_id`, ตำแหน่ง/กฎ, `category`, `confidence`, `reason`, `author`, `commit` และ `contributors` แต่ละ contributor มี commit, parent SHAs, author, ประเภท/confidence/reason และรายชื่อบรรทัดที่เกี่ยวข้อง Finding จากหลาย commit ของ author เดียวกันและประเภทเดียวกันรวมเป็นเจ้าของเดียวได้ โดยยังเก็บทุก contributor

- `high`: ประวัติ Git ครบและระบุ topology/ผู้แก้ไขล่าสุดได้สอดคล้องกัน **ไม่ใช่การยืนยันผู้เขียนต้นฉบับหรือคะแนนส่วนบุคคล**
- `low`: blame ชี้ไปที่ merge commit (รวมการแก้ conflict ด้วยมือ), พบสัญญาณ squash/หลาย author ในข้อความ commit, ข้อมูล identity ไม่ครบ หรือ attribution เป็น unknown ต้องตรวจเพิ่มเติมก่อนนำไปนับคะแนนรายคน
- สัญญาณ squash ได้แก่ข้อความ `squash`/`squashed`/`squashing`, trailer `Co-authored-by:` และรูปแบบ `(#<PR number>)` ที่ท้ายบรรทัด การตรวจนี้เลือกความระมัดระวัง จึงอาจให้ low กับ commit ปกติที่มีข้อความลักษณะเดียวกัน
- Squash ที่ไม่มีข้อความบ่งชี้, rebase/cherry-pick ที่ลบร่องรอย, fast-forward import ที่ไม่มี merge parent และการสลับลำดับ parent ไม่มีหลักฐานใน graph เพียงพอให้แยกเจ้าของต้นฉบับได้แน่นอน อาจถูกจัดตามประวัติหลักที่เหลืออยู่ ต้องดู provenance เพิ่มเติม/ให้ผู้รีวิวตรวจ ไม่รับประกันว่า high แปลว่าไม่เคย squash
- Merge ที่แก้ conflict แต่เลือกบรรทัดเดิมของ parent โดยไม่เปลี่ยน bytes อาจยัง blame ไปที่ author ของ parent ตาม Git; ระบบระบุ low เมื่อ blame ชี้ไปยัง merge commit ไม่สามารถยืนยันเจตนาการเลือกบรรทัดเดิมนั้นได้
- ถ้า finding ครอบคลุมหลาย author/หลายประเภท จะเป็น `unknown + low`, `author=null` พร้อม contributors แยก และถูกนับเพียงหนึ่งครั้งในกลุ่ม Unresolved ไม่แจก violation เดียวซ้ำให้หลายคน
- Finding นอกช่วง diff ยังคงอยู่ใน attribution และรายงานเดิม โดย `category=not_in_scope`, `confidence=null`, `reason=outside_changed_lines`, `author=null` และ contributors ว่าง ไม่นับรวมใน `unknown`, confidence high/low หรือ summary รายคน
- Author identity ใช้ Git email ที่ hash ด้วย SHA-256 เพื่อแยกคนชื่อซ้ำ ไม่บันทึก email ดิบ ไม่เดา login จากชื่อ Git ผู้เปิด PR อยู่ใน `pull_request.author` แยกต่างหาก ดูแหล่งที่มาของ login ด้านล่าง
- Checkout ใช้ `fetch-depth: 0` อยู่แล้ว ถ้า finalize พบ shallow repository หรือหา base/head/parents ไม่ได้ จะบันทึก warning และใช้ `unknown + low` แทนการ crash ไม่มีการ fetch เพิ่มอัตโนมัติใน finalize; หากเตรียม diff ไม่ได้ตั้งแต่ prepare จะรายงาน error และไม่เดา diff ขึ้นมา

`attribution.json.summary` เก็บจำนวน raw/changed-line/outside-diff, จำนวนตามประเภท/confidence/กฎ และ `people` ที่มี `by_category`, `by_confidence`, `by_rule`, `rules_by_category` ตารางใน `summary.md` แสดงข้อควรระวังเรื่อง confidence ต่ำอย่างชัดเจน Attribution ไม่เปลี่ยนผล check และไม่มีการคำนวณคะแนนบุคคลอัตโนมัติ

## Report schema 2 และความเข้ากันได้ย้อนหลัง

รายงานที่สร้างใหม่ใช้ `schema_version=2` (attribution, metadata, status, manifest และ aggregate summary) Native `pmd.json`/`pmd.xml` ยังคงรูปแบบของ PMD ส่วน `violations.json` ยังคงเป็นรายการ findings ดิบครบเหมือนเดิม ชื่อไฟล์ทั้งหมดใน Artifact ไม่เปลี่ยน

`attribution.json.check_counts` และ `status.json.counts` เปลี่ยนดังนี้:

| Schema 1 | Schema 2 | ขอบเขตที่นับ |
| --- | --- | --- |
| `violations` | `raw_violations` | Findings ดิบทั้งหมดในไฟล์ที่ตรวจ |
| `on_changed_lines` | `gating_violations` | Findings ที่ทับบรรทัดเปลี่ยน ใช้ตัดสิน check |
| ไม่มี | `outside_changed_lines` | Findings นอกบรรทัดเปลี่ยน ไม่มีผลต่อ check |
| `by_rule` | `raw_by_rule` | ทั้งหมด แยกตามกฎ |
| `by_priority` | `raw_by_priority` | ทั้งหมด แยกตาม priority |
| `by_file` | `raw_by_file` | ทั้งหมด แยกตามไฟล์ |
| ไม่มี | `gating_by_rule` | เฉพาะ findings ที่มีผลต่อ check แยกตามกฎ |

ต้องได้ `raw_violations = gating_violations + outside_changed_lines` ส่วน `errors` และ `suppressed` เก็บเหมือนเดิม ข้อผิดพลาดเครื่องมือ/รายงานยังทำให้ check เป็น `error` ได้ตามเกณฑ์เดิม

`check_scope` (และ `status.json.gate_scope`) เปลี่ยนจาก `all_violations_overlapping_changed_lines` เป็น `violations_overlapping_changed_lines` เพิ่ม `check_scope_description` / `gate_scope_description` อธิบายว่ารายการนอก diff เป็นข้อมูลอย่างเดียว ไม่เปลี่ยนเกณฑ์ check: attribution เป็น imported/unknown หรือ confidence ต่ำยังมีผลเมื่อทับบรรทัดเปลี่ยน

`summary.by_category` มี `own`, `imported`, `unknown`, `not_in_scope` โดยสามประเภทแรกนับเฉพาะในขอบเขต และ `not_in_scope` เท่ากับจำนวนด้านนอก `summary.by_confidence`, `summary.by_rule` และ `people` นับเฉพาะในขอบเขต ชื่อยอด summary เดิม (`total_raw_violations`, `changed_line_violations`, `outside_changed_lines`) ยังคงไว้พร้อมขอบเขตนี้ `people[].by_category` มีเฉพาะ own/imported/unknown เพราะไม่ได้แจก findings นอกขอบเขตให้คนใด

`pmd_aggregate.py` อ่านทั้ง schema 1 และ 2 พร้อมกัน แปลง schema 1 **ในหน่วยความจำ**: finding ที่ไม่ทับ diff และ `reason=outside_changed_lines` เป็น `not_in_scope + confidence=null` และปรับชื่อ counts/scope เก่า ไม่เขียนทับรายงานที่ดาวน์โหลด ไม่เอา unknown ที่ระบุเจ้าของไม่ได้จริงไปปนกับรายการนอกขอบเขต เลือกผลล่าสุดต่อ PR เหมือนเดิม และออกรายงาน schema 2 พร้อมคอลัมน์ `not_in_scope` เพิ่มท้าย `pull-requests.csv`

ผู้อ่าน JSON ภายนอกต้องตรวจ schema_version และเปลี่ยน field mapping ตามตารางก่อนอ่าน schema 2 ไม่ต้องแปลงคลัง schema 1 ล่วงหน้า หากย้อนเวอร์ชัน workflow ให้คงรายงาน schema 2 ที่เก็บแล้วและใช้ aggregator รุ่นนี้อ่านทั้งสองรุ่น ตัวอ่านเก่าที่รองรับเฉพาะ schema 1 จะอ่าน schema 2 ไม่ได้

## แหล่งที่มาของ GitHub login

- GitHub event `pull_request.user.login` คือ **ผู้เปิด PR** บันทึกเป็น `pull_request.author` ไม่ใช่หลักฐานผู้เขียนแต่ละ commit เช่นเดียวกับ `GITHUB_ACTOR` ซึ่งเป็นผู้กระตุ้น run จึงไม่ใช้สองค่านี้เติม `findings[].author.github_login`
- บน GitHub.com Actions workflow ให้ token อัตโนมัติที่มีเพียง `contents: read` แก่ขั้น finalize ผ่าน `PMD_GITHUB_TOKEN` ใช้ REST `GET /repos/{owner}/{repo}/commits/{sha}` อ่าน account ที่ GitHub ผูกกับ **author** ของ commit (ไม่ใช่ committer) ต้องได้ SHA ตรงกันและ Git author email ตรงกับ commit ในเครื่องก่อนเติม `github_login_source=github_commit_api` ไม่บันทึก token, response API หรือ email ดิบ และไม่เปลี่ยน author ID เดิมเพื่อให้รวมกับรายงานเก่าได้
- หากไม่มี API account association หรือ API ใช้ไม่ได้ จะอ่านเฉพาะ email รูปแบบ `ID+username@users.noreply.github.com` หรือ `username@users.noreply.github.com` ที่ GitHub ระบุไว้ แล้วให้ `github_login_source=github_noreply_email` นี่คือ username ที่บันทึกใน commit อาจเป็นชื่อเก่าหลังเปลี่ยนบัญชี และไม่ใช่หลักฐานว่าผู้ commit ไม่ได้ปลอม author
- หากไม่มีแหล่งที่มาทั้งสอง จะเก็บ `github_login=null`, `github_login_source=null` แม้ชื่อ Git ตรงกับผู้เปิด PR ก็ตาม Email ปกติยังใช้ hash เดิม ไม่มีการค้นผู้ใช้จากชื่อ Git/email หรือแจก violation ให้ opener
- API lookup ใช้เฉพาะ Actions ที่มี token และ GitHub.com API ตรวจแต่ละ commit ครั้งเดียวผ่าน cache ของ attribution ไม่ตาม redirect ตั้ง timeout 5 วินาที/คำขอ สูงสุด 100 commit ต่อ run และหยุด lookup ต่อเมื่อ network/rate limit/response ใช้งานไม่ได้พร้อม warning ส่วน blame/check ทำงานต่อเหมือนเดิม GitHub Enterprise API ยังไม่รองรับ lookup (ใช้ noreply/null)
- การรันในเครื่องโดยไม่มี Actions token จะใช้ noreply/null ทดสอบ API ด้วย mock ได้ แต่ยังต้องยืนยัน account association ของ repository จริงจาก run บน Actions ค่า login เป็นการเชื่อมบัญชีที่ GitHub รู้จัก ไม่เพิ่ม confidence ของ provenance และไม่ยืนยันเจ้าของต้นฉบับหลัง squash

แหล่งอ้างอิง: [GitHub commit API และ author.login](https://docs.github.com/en/rest/commits/commits#get-a-commit), [GitHub noreply email formats](https://docs.github.com/en/account-and-profile/reference/email-addresses-reference)

## รวม summary ข้าม PR เมื่อพร้อมใช้งาน

ดาวน์โหลดและแตก ZIP ของ run ที่ต้องการก่อน แล้วรันจาก root:

```bash
python scripts/pmd_aggregate.py /path/to/downloaded-pmd-artifacts \
  --output backend/target/pmd-summary

# หรือส่งหลาย attribution.json โดยตรง
python scripts/pmd_aggregate.py /path/pr-1/attribution.json /path/pr-2/attribution.json \
  --output backend/target/pmd-summary
```

อ่าน `attribution.json` ใต้ input directory แบบ recursive เลือก **หนึ่ง run ต่อ repository + เลข PR** โดยเรียง GitHub run ID แล้ว attempt ก่อน timestamp เพื่อไม่ให้ rerun PR head เก่าที่จบทีหลังไปแทน run ใหม่ หากไม่มี run ID จะใช้เวลาเริ่ม UTC ของ run ที่บันทึกไว้ ไม่ใช้เวลาที่ดาวน์โหลดไฟล์

Output: `summary.json`, `summary.md`, `people.csv`, `pull-requests.csv` สรุปแยก Git author/own/imported/unknown/confidence/กฎ และแยก PR opener กับผล check ผ่าน/ไม่ผ่าน/error/no Java ออกจากกัน ไม่เอา violations ของทุก author ไปนับเป็นของผู้เปิด PR

ตัวรวมเลือกผลล่าสุด **จากข้อมูลที่ส่งเข้ามาเท่านั้น** ไม่ได้ query GitHub เพื่อยืนยัน head ปัจจุบันหรือค้นหา run ที่ถูกยกเลิก/หายไป หาก archive ไม่ครบ รายงานก็ไม่ครอบคลุมทุก PR ของ repo ต้องตรวจรายชื่อ PR/run/head SHA ใน output ด้วย ข้อมูล local ที่ไม่มีเลข PR จริงจะถูกปฏิเสธเพื่อไม่รวมทุก null PR เข้าด้วยกัน Input เสียจะออก partial summary พร้อม `complete=false`, `errors` และ exit code 1

จำนวนรวมเป็นจำนวน findings ที่ปรากฏใน PR ที่เลือก ไม่ใช่จำนวน defect ที่ไม่ซ้ำทั้ง repo หาก defect เดียวถูกนำเข้าไปปรากฏในหลาย PR อาจนับหลาย occurrences แต่ยังแยก imported และไม่โยนให้ opener

## ขั้นต่อไปสำหรับการเก็บถาวร (ยังไม่ทำอัตโนมัติ)

1. ก่อน Artifact หมดอายุ 90 วัน ดาวน์โหลด ZIP ทั้งชุดจาก Actions ไม่ใช่เฉพาะ Summary เช่นใช้หน้าเว็บ หรือ `gh run download <run-id> -n <artifact-name> -D <archive-directory>` เมื่อมี GitHub CLI และสิทธิ์อ่าน repo
2. เก็บในคลังถาวรของทีมที่มีสิทธิ์เทียบเท่า repo แยก `<repository>/<PR number>/<run ID>-<attempt>/` คงไฟล์เดิมทั้งหมดและ `manifest.json`; หลีกเลี่ยงใช้ `backend/target` เป็นคลังถาวร เพราะ Maven clean ลบได้
3. ตรวจ SHA-256 ใน manifest หลังคัดลอก และสำรองคลังตามนโยบายทีม อย่าบันทึก token/secret ปนกับรายงาน
4. เรียก `pmd_aggregate.py` กับคลังนี้เมื่อต้องการ summary ภายหลัง ไม่ต้องมี Git history ในเครื่องสำหรับ aggregation เพราะ attribution บันทึกไว้แล้ว
5. ถ้าต้องการทำ archive อัตโนมัติภายหลัง ต้องเลือกปลายทาง เช่น private object storage และกำหนดสิทธิ์/credential/retention/backup ก่อนเพิ่ม workflow แยก งานนี้ยังไม่เชื่อม storage, ไม่สร้าง scheduled archive และไม่แก้ branch protection

## เปิดใช้งานบน GitHub

1. Push ไฟล์เหล่านี้และเปิด PR เข้าสู่ `develop` หรือ `main` ให้ workflow รันอย่างน้อยหนึ่งครั้ง
2. เข้า Settings → Rules → Rulesets หรือ Branch protection rules สำหรับแต่ละ branch ปลายทาง
3. เปิด Require status checks to pass before merging และเลือก **`pmd-scan`** โดยเลือก GitHub Actions เป็นแหล่งของ check ถ้าหน้าตั้งค่ารองรับ
4. PR ที่มี violation ตรงกับบรรทัดเปลี่ยนต้องแก้ source ก่อน merge; PMD tool failure ต้องแก้ที่เครื่องมือหรือ config ไม่ใช้ `continue-on-error`/skip เพื่อลบสถานะแดง

ไฟล์ workflow ไม่สามารถเปิด branch protection เอง ต้องตั้งด้วยสิทธิ์ admin ของ GitHub Repository สถานะหลักที่รับประกันคือ check ของ PR; หน้า Branches อาจแสดงตาม commit/check context ที่ GitHub เลือก ไม่ควรใช้คอลัมน์นั้นเป็นฐานข้อมูลผลตรวจ และ branch ที่ไม่มี PR เข้า `main`/`develop` จะไม่ถูกรัน workflow นี้ หากใช้ Merge Queue ต้องเพิ่มการรองรับ `merge_group` ก่อนบังคับใช้ check กับ queue

Workflow ใช้ `pull_request` พร้อม `contents: read`, ใช้ token อัตโนมัติของ Actions สำหรับอ่าน commit author, ไม่ใช้ `pull_request_target`, ไม่โพสต์ comment, ไม่เขียน branch และไม่ใช้ custom repository secrets รองรับ read-only token ของ fork PR ภายใต้นโยบายอนุมัติ workflow ของ repository

## ตรวจส่วน PMD ในเครื่อง

```bash
python -m unittest discover -s scripts/tests -p 'test_pmd*.py' -v

# รันจาก root ที่ checkout ตรงกับ PR head และไฟล์ Java ไม่มีการแก้ไขค้าง
export PMD_VERSION=7.10.0
export PMD_SHA256=cd676b19dbe87e86f7eed5940a484be2d3ab6a2d1d552e605860ab12fbf05cbb
export PMD_REPORT_DIR=backend/target/pmd-diff
python scripts/pmd_diff.py prepare --base <base-commit-sha> --head <head-commit-sha>
export PMD_BIN=/absolute/path/to/pmd-bin-7.10.0/bin/pmd
bash scripts/pmd-scan.sh
# ต้อง finalize แม้ scan จะคืนค่าไม่เป็นศูนย์
python scripts/pmd_diff.py finalize
```

PMD ZIP ตรวจด้วย SHA-256 ที่ pin ไว้ก่อนแตกไฟล์ GitHub Actions pin ด้วย commit SHA เพื่อไม่เปลี่ยนเครื่องมือโดยไม่ตั้งใจ ไม่เปลี่ยน dependencies ของแอปหรือ `backend/pom.xml` และไม่แตะ security workflow/gate

การตรวจว่าไฟล์ตรงกับ PR head ใช้ Git blob hash หลัง Git normalize line endings จึงรองรับ Windows CRLF และยังบันทึก SHA-256 ของ bytes ที่ PMD อ่านจริง เมื่อรันในเครื่องซ้ำ จะปฏิเสธรายงานเก่าหรือ metadata จาก prepare ที่ล้มเหลว เพื่อไม่ให้แสดงผลผ่านจาก run ก่อน

เอกสารอ้างอิง: [PMD CLI](https://pmd.github.io/pmd/pmd_userdocs_cli_reference.html), [Git blame](https://git-scm.com/docs/git-blame), [Git rev-list](https://git-scm.com/docs/git-rev-list), [Git merge-base](https://git-scm.com/docs/git-merge-base), [GitHub Artifacts](https://docs.github.com/en/actions/how-tos/writing-workflows/choosing-what-your-workflow-does/storing-and-sharing-data-from-a-workflow), [Required checks ที่ถูก skip](https://docs.github.com/en/pull-requests/how-tos/merge-and-close-pull-requests/troubleshooting-required-status-checks)
