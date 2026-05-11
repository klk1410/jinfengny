export async function requestJson(url, options) {
  const res = await fetch(url, options);
  const json = await res.json();
  if (json.code !== 200) {
    throw new Error(json.message || "请求失败");
  }
  return json.data;
}
