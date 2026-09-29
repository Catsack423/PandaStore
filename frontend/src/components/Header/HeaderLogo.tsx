import Image from "next/image";
import Link from "next/link";

export default function HeaderLogo({ href }: { href: string }) {
  return <Link className="flex-shrink-0" href={href}>
    <Image src="/images/logo/logo.svg" alt="Logo" width={219} height={36} />
  </Link>;
}
